"""[사용 불가 — 보관용] MCM Korea 상품 페이지를 HTTP로 직접 수집하던 스크립트.

2026-08-18 기준 kr.mcmworldwide.com 은 Akamai 봇 차단이 걸려 있어 sitemap.xml,
robots.txt, 상품 상세까지 전부 403 을 돌려준다. User-Agent/헤더를 브라우저와
동일하게 맞춰도, curl 로 바꿔도 동일하다(TLS 핑거프린팅). 즉 이 스크립트는
어떤 수정으로도 되살릴 수 없다.

현재 수집 절차는 실제 Chrome 탭 안에서 fetch 를 돌리는 방식이다:
    1) scripts/mcm_crawl_browser.js  — 브라우저에서 수집 → JSONL 다운로드
    2) scripts/jsonl_to_csv.py       — JSONL → data/mcm-products-full.csv 병합
    3) scripts/csv_to_seed.py        — CSV → src/main/resources/seed/mcm-products.json

아래 코드는 사이트 차단이 풀렸을 때의 참고용으로만 남겨 둔다.
"""

import csv
import json
import re
import time
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin
from urllib.request import Request, urlopen
from xml.etree import ElementTree

BASE = Path(__file__).parent
DATA = BASE / "data"
CSV_PATH = DATA / "mcm-products-full.csv"
OUT_PATH = DATA / "mcm-products-crawled.csv"
SITE = "https://kr.mcmworldwide.com"
CATEGORY_URLS = [
    ("여성", "CLOTHING", f"{SITE}/ko_KR/%EC%97%AC%EC%84%B1/%EC%9D%98%EB%A5%98/%EB%AA%A8%EB%91%90%EB%B3%B4%EA%B8%B0"),
    ("남성", "CLOTHING", f"{SITE}/ko_KR/%EB%82%A8%EC%84%B1/%EC%9D%98%EB%A5%98/%EB%AA%A8%EB%91%90%EB%B3%B4%EA%B8%B0"),
    ("여성", "BAG", f"{SITE}/ko_KR/%EC%97%AC%EC%84%B1/%EA%B0%80%EB%B0%A9/%EB%AA%A8%EB%91%90%EB%B3%B4%EA%B8%B0"),
    ("남성", "BAG", f"{SITE}/ko_KR/%EB%82%A8%EC%84%B1/%EA%B0%80%EB%B0%A9/%EB%AA%A8%EB%91%90%EB%B3%B4%EA%B8%B0"),
    ("여성", "SHOES", f"{SITE}/ko_KR/%EC%97%AC%EC%84%B1/%EC%8A%88%EC%A6%88/%EB%AA%A8%EB%91%90%EB%B3%B4%EA%B8%B0"),
    ("남성", "SHOES", f"{SITE}/ko_KR/%EB%82%A8%EC%84%B1/%EC%8A%88%EC%A6%88/%EB%AA%A8%EB%91%90%EB%B3%B4%EA%B8%B0"),
]
HEADERS = {
    "User-Agent": "Mozilla/5.0 (compatible; MCM-Muse-Catalog/1.0; +https://kr.mcmworldwide.com)",
    "Accept-Language": "ko-KR,ko;q=0.9,en;q=0.8",
}


def get(url: str) -> bytes:
    req = Request(url, headers=HEADERS)
    with urlopen(req, timeout=30) as response:
        return response.read()


def product_urls_from_sitemap(xml: bytes) -> list[str]:
    root = ElementTree.fromstring(xml)
    urls = []
    for node in root.iter():
        if node.tag.endswith("loc") and node.text:
            value = node.text.strip()
            if value.startswith(SITE) and re.search(r"/[A-Z0-9]{10,}\.html(?:$|\?)", value):
                urls.append(value)
    return sorted(set(urls))


def product_urls_from_category(html: str) -> list[str]:
    """PLP HTML의 상품 상세 링크를 추출한다. 페이지네이션은 호출부에서 처리한다."""
    links = re.findall(r'href=["\']([^"\']+\.html(?:\?[^"\']*)?)["\']', html, re.I)
    return sorted({urljoin(SITE, link.split("#", 1)[0]) for link in links if ".html" in link})


def json_ld_product(html: str) -> dict | None:
    blocks = re.findall(
        r'<script[^>]+type=["\']application/ld\+json["\'][^>]*>(.*?)</script>',
        html,
        flags=re.I | re.S,
    )
    for block in blocks:
        try:
            value = json.loads(block.strip())
        except json.JSONDecodeError:
            continue
        candidates = value if isinstance(value, list) else [value]
        for item in candidates:
            if isinstance(item, dict) and "Product" in str(item.get("@type")):
                return item
    return None


def first(value):
    return value[0] if isinstance(value, list) and value else value


def parse_product(url: str, html: str, gender: str = "", group: str = "") -> dict | None:
    product = json_ld_product(html) or {}
    match = re.search(r"([A-Z][A-Z0-9]{9,})\.html", url)
    sku = product.get("sku") or (match.group(1) if match else "")
    if not sku or not product.get("name"):
        return None
    offers = product.get("offers") or {}
    offers = first(offers) or {}
    images = product.get("image") or []
    images = images if isinstance(images, list) else [images]
    images = [urljoin(url, image) for image in images if image]
    return {
        "pid": sku,
        "name": str(product.get("name", "")).strip(),
        "priceValue": int(float(str(offers.get("price", "0")).replace(",", "") or 0)),
        "color": product.get("color", ""),
        "gender": gender,
        "group": group or infer_group(url, product.get("name", "")),
        "availability": "InStock",
        "shortDesc": str(product.get("description", "")).strip(),
        "longDesc": str(product.get("description", "")).strip(),
        "material": product.get("material", ""),
        "dimensions": "",
        "strap": "",
        "madeIn": "",
        "features": "",
        "sizes": "",
        "images": " | ".join(images) or f"https://images.mcmworldwide.com/i/mcmworldwide/{sku}_01?sw=600",
        "url": product.get("url") or url,
    }


def infer_group(url: str, name: str) -> str:
    path = url.lower()
    if any(word in path for word in ("의류", "clothing", "티셔츠", "재킷", "팬츠", "스커트")):
        return "CLOTHING"
    if any(word in path for word in ("슈즈", "shoes", "스니커", "샌들", "부츠")):
        return "SHOES"
    if any(word in path for word in ("지갑", "wallet", "카드", "벨트", "참", "액세서리")):
        return "ACCESSORY"
    return "BAG"


def collect_urls() -> list[tuple[str, str, str]]:
    """사이트맵 + 성별/상품군 PLP를 합쳐 카테고리 누락을 방지한다."""
    collected: dict[str, tuple[str, str, str]] = {}
    try:
        for url in product_urls_from_sitemap(get(f"{SITE}/sitemap.xml")):
            collected[url] = (url, "", infer_group(url, ""))
    except (HTTPError, URLError, TimeoutError) as error:
        print(f"sitemap unavailable: {error}")
    for gender, group, category_url in CATEGORY_URLS:
        for page in range(0, 20):
            page_url = category_url if page == 0 else f"{category_url}?page={page}"
            try:
                html = get(page_url).decode("utf-8", errors="replace")
            except (HTTPError, URLError, TimeoutError) as error:
                print(f"category unavailable {page_url}: {error}")
                break
            links = product_urls_from_category(html)
            before = len(collected)
            for url in links:
                collected[url] = (url, gender, group)
            if not links or len(collected) == before:
                break
            time.sleep(0.5)
    return sorted(collected.values())


def main() -> None:
    urls = collect_urls()
    if not urls:
        raise SystemExit("MCM 사이트맵/여성·남성 카테고리에서 공개 상품 URL을 찾지 못했습니다.")

    with CSV_PATH.open(encoding="utf-8-sig", newline="") as handle:
        old_rows = list(csv.DictReader(handle))
    known = {row["pid"] for row in old_rows if row.get("pid")}
    new_rows = []
    for index, (url, gender, group) in enumerate(urls, start=1):
        sku_match = re.search(r"/([A-Z][A-Z0-9]{9,})\.html", url)
        if sku_match and sku_match.group(1) in known:
            continue
        try:
            row = parse_product(url, get(url).decode("utf-8", errors="replace"), gender, group)
        except (HTTPError, URLError, TimeoutError) as error:
            print(f"skip {index}/{len(urls)} {url}: {error}")
            continue
        if row and row["pid"] not in known:
            known.add(row["pid"])
            new_rows.append(row)
        time.sleep(0.5)

    fields = list(old_rows[0].keys()) if old_rows else list(new_rows[0].keys())
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    with OUT_PATH.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(old_rows + new_rows)
    print(f"공식 URL {len(urls)}개 / 신규 {len(new_rows)}개 / 병합 {len(old_rows) + len(new_rows)}개")
    print(f"결과: {OUT_PATH}")


if __name__ == "__main__":
    main()
