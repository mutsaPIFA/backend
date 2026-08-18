/* MCM 공식몰 상품 수집기 — 실제 Chrome 탭 안에서 실행한다.
 *
 * 배경: kr.mcmworldwide.com 은 Akamai 봇 차단으로 python/curl 요청을 403 처리한다
 * (2026-08-18 확인). 그래서 수집은 로그인된 실제 브라우저 컨텍스트의 fetch 로만 가능하다.
 *
 * 사용법 (Chrome DevTools 콘솔 또는 자동화 도구에서):
 *   1) https://kr.mcmworldwide.com/ko_KR/home 접속
 *   2) 이 파일 내용을 붙여넣어 MCMCrawl 을 등록
 *   3) await MCMCrawl.loadSitemap()            // 상품 URL 수집
 *      MCMCrawl.plan({clothingAll:true, ratio:0.5})
 *      MCMCrawl.start()                        // 백그라운드 수집 시작
 *      MCMCrawl.progress()                     // 진행률
 *      MCMCrawl.take(20)                       // 수집분 20건 꺼내기(꺼낸 건 큐에서 제거)
 *
 * 상품 1건 = 색상 변형 1개(스타일코드 11자리). 사이즈는 sizes 필드로 합친다.
 */
(() => {
  const SITE = "https://kr.mcmworldwide.com";
  const GEN = { 여성: "WOMEN", 남성: "MEN", women: "WOMEN", men: "MEN" };
  const CAT = {
    의류: "CLOTHING",
    clothing: "CLOTHING",
    핸드백: "BAG",
    가방: "BAG",
    bags: "BAG",
    슈즈: "SHOES",
    shoes: "SHOES",
    패션소품: "ACCESSORY",
    "지갑-레더소품": "WALLET",
  };
  // dataLayer page_id → 상품군 (사이트맵 경로가 캠페인 URL뿐인 상품 보정용)
  const PAGE_ID = [
    [/clothing|apparel|top|bottom|outerwear|dress|knitwear|sweatshirt/, "CLOTHING"],
    [/shoe|sneaker|sandal|boot|loafer/, "SHOES"],
    [/wallet|leather-good|card-case/, "WALLET"],
    [/bag|backpack|tote|crossbody|luggage|trolley|trunk/, "BAG"],
    [/accessor|belt|charm|jewel|scarf|hat|tech|pet|home/, "ACCESSORY"],
  ];

  const state = {
    targets: [], // {base, url, gender, group}
    done: [], // 수집 완료(아직 안 꺼낸 것)
    failed: [],
    started: 0,
    finished: 0,
    running: false,
  };

  const text = (el) => (el ? el.textContent.replace(/\s+/g, " ").trim() : "");

  function jsonLd(doc) {
    for (const s of doc.querySelectorAll('script[type="application/ld+json"]')) {
      let v;
      try {
        v = JSON.parse(s.textContent.trim());
      } catch {
        continue;
      }
      for (const item of Array.isArray(v) ? v : [v]) {
        if (item && String(item["@type"] || "").includes("Product")) return item;
      }
    }
    return null;
  }

  function dataLayerProduct(html) {
    const m = html.match(/"productView"[\s\S]{0,1200}?"products":\[(\{[\s\S]{0,900}?\})\]/);
    if (!m) return {};
    try {
      return JSON.parse(m[1].replace(/&quot;/g, '"'));
    } catch {
      return {};
    }
  }

  function pageId(html) {
    const m = html.match(/"page_id"\s*:\s*"([^"]+)"/);
    return m ? m[1] : "";
  }

  // 소재 라벨은 제품군/언어마다 다르다(가방 바디·트림 / 슈즈 어퍼·아웃솔 /
  // 참 앞면·뒷면 / 영문 상품 Body·Trim). 향수·선글라스처럼 소재 표기가 아예
  // 없는 상품군도 있어 빈 값은 정상이다.
  const MATERIAL_LABEL =
    /^(바디|트림|소재|겉감|안감|앞면|뒷면|어퍼|아웃솔|인솔|라이닝|body|trim|upper|outsole|insole|lining|material|fabric)\s*[::]/i;

  // bullet 목록을 소재/치수/스트랩/제조국/기타 특징으로 나눈다.
  function splitBullets(bullets) {
    const out = { material: [], dimensions: "", strap: "", madeIn: "", features: [] };
    for (const b of bullets) {
      if (MATERIAL_LABEL.test(b)) out.material.push(b);
      else if (/^제조국\s*[::]/.test(b)) out.madeIn = b.replace(/^제조국\s*[::]\s*/, "");
      else if (/스트랩 길이|핸들 드롭|strap length|handle drop/i.test(b)) out.strap = b;
      else if (/^약?\s*[\d.]+\s*[xX×]\s*[\d.]+/.test(b) || /센티미터|centimeters/.test(b)) out.dimensions = b;
      else out.features.push(b);
    }
    return out;
  }

  function parse(url, html, hint) {
    const doc = new DOMParser().parseFromString(html, "text/html");
    const ld = jsonLd(doc);
    const m = url.match(/\/([A-Z][A-Z0-9]{9,})\.html/);
    const sku = (ld && ld.sku) || (m ? m[1] : "");
    if (!sku || !ld || !ld.name) return null;

    const dl = dataLayerProduct(html);
    const bullets = [...doc.querySelectorAll("ul.dotted-list.bullet-points li")].map((e) =>
      text(e)
    );
    const parts = splitBullets(bullets);
    // PDP에 모바일/데스크톱 사이즈 셀렉터가 중복 렌더링돼 중복 제거가 필요하다.
    const sizes = [
      ...new Set(
        [...doc.querySelectorAll("[data-attr=size] option")]
          .map((o) => text(o))
          .filter((s) => s && s !== "사이즈 선택")
      ),
    ];
    // 이미지는 {SKU}_{NN} 규칙이라 접미사만 넘기고 CSV 단계에서 URL을 복원한다.
    const imgs = (Array.isArray(ld.image) ? ld.image : [ld.image])
      .filter(Boolean)
      .map((u) => {
        const mm = String(u).match(/_(\d{2})\?/);
        return mm ? mm[1] : null;
      })
      .filter(Boolean);

    const offers = Array.isArray(ld.offers) ? ld.offers[0] : ld.offers || {};
    const price = Math.round(parseFloat(String(offers.price || dl.price || 0).replace(/,/g, "")) || 0);

    let group = hint.group;
    if (!group) {
      const pid = pageId(html);
      for (const [re, g] of PAGE_ID) if (re.test(pid)) { group = g; break; }
    }
    let gender = hint.gender;
    if (!gender) {
      const pid = pageId(html) + " " + url;
      gender = /women|여성|ladies/i.test(pid) ? "WOMEN" : /men|남성/i.test(pid) ? "MEN" : "";
    }

    return {
      pid: sku,
      base: sku.slice(0, 11),
      name: String(ld.name || "").trim(),
      priceValue: price,
      color: ld.color || dl.Color || "",
      gender,
      group: group || "",
      availability: String(offers.availability || "").includes("OutOfStock") ? "OutOfStock" : "InStock",
      shortDesc: text(doc.querySelector(".js-product-short-description")),
      longDesc: String(ld.description || "").trim(),
      material: parts.material.join(" | "),
      dimensions: parts.dimensions,
      strap: parts.strap,
      madeIn: parts.madeIn,
      features: parts.features.join(" | "),
      sizes: sizes.join(" | "),
      imgs: imgs.join(","),
      path: decodeURIComponent(new URL(url).pathname),
    };
  }

  async function fetchOne(t) {
    const res = await fetch(t.url, { credentials: "include" });
    if (!res.ok) throw new Error("HTTP " + res.status);
    return parse(t.url, await res.text(), t);
  }

  const MCMCrawl = {
    state,

    async loadSitemap() {
      const xml = await (await fetch(SITE + "/sitemap-pages-ko-kr-PRODUCT-1.xml")).text();
      const locs = [...xml.matchAll(/<loc>([^<]+)<\/loc>/g)].map((m) => m[1]);
      const base = new Map();
      for (const u of locs) {
        const m = u.match(/\/([A-Z][A-Z0-9]{9,})\.html$/);
        if (!m) continue;
        const b = m[1].slice(0, 11);
        const seg = decodeURIComponent(new URL(u).pathname).split("/").filter(Boolean);
        const gender = GEN[seg[1]] || "";
        const group = CAT[seg[2]] || "";
        const prev = base.get(b);
        if (!prev) base.set(b, { base: b, url: u, gender, group });
        else if (gender && group && !(prev.gender && prev.group))
          base.set(b, { base: b, url: u, gender, group });
      }
      state.all = [...base.values()];
      return { products: state.all.length, urls: locs.length };
    },

    // known: 이미 CSV에 있는 base 코드 배열 (재수집 제외)
    plan({ known = [], quotas = {}, clothingAll = true } = {}) {
      const have = new Set(known);
      const pool = state.all.filter((t) => !have.has(t.base));
      const picked = [];
      const byGroup = {};
      for (const t of pool) (byGroup[t.group || "?"] ||= []).push(t);
      for (const [g, list] of Object.entries(byGroup)) {
        const n = g === "CLOTHING" && clothingAll ? list.length : quotas[g] ?? 0;
        picked.push(...list.slice(0, n));
      }
      state.targets = picked;
      const counts = {};
      for (const t of picked) counts[(t.gender || "?") + "/" + (t.group || "?")] = (counts[(t.gender || "?") + "/" + (t.group || "?")] || 0) + 1;
      return { planned: picked.length, counts };
    },

    setTargets(list) {
      state.targets = list;
      return list.length;
    },

    start(concurrency = 5) {
      if (state.running) return "already running";
      state.running = true;
      let i = 0;
      const worker = async () => {
        while (i < state.targets.length) {
          const t = state.targets[i++];
          state.started++;
          try {
            const row = await fetchOne(t);
            if (row) state.done.push(row);
            else state.failed.push({ base: t.base, why: "no-jsonld" });
          } catch (e) {
            state.failed.push({ base: t.base, why: String(e).slice(0, 80) });
          }
          state.finished++;
        }
      };
      Promise.all(Array.from({ length: concurrency }, worker)).then(() => {
        state.running = false;
      });
      return "started " + state.targets.length;
    },

    progress() {
      return {
        planned: state.targets.length,
        finished: state.finished,
        queued: state.done.length,
        failed: state.failed.length,
        running: state.running,
      };
    },

    take(n = 25) {
      return state.done.splice(0, n);
    },
  };

  window.MCMCrawl = MCMCrawl;
  return "MCMCrawl ready";
})();
