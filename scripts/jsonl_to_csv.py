"""브라우저 크롤러(mcm_crawl_browser.js) 산출 JSONL → 기존 상품 CSV에 병합.

사용법:
    python scripts/jsonl_to_csv.py [JSONL경로]
    (기본 입력: scripts/data/mcm-crawl-raw.jsonl
     기준 CSV: scripts/data/mcm-products-full.csv → 출력: 같은 파일에 덮어쓰기 전 .bak 보관)

JSONL 한 줄 = 색상 변형 1건. CSV 스키마로 복원하는 항목:
- images: imgs("01,09,02") + pid → 공식 이미지 CDN URL 로 복원
- url:    path → 사이트 도메인 결합
- sizes:  PDP에 모바일/데스크톱 셀렉터가 중복 존재해 중복 사이즈를 제거
- group:  캠페인 URL에만 존재해 상품군 판별이 안 된 건은 SKU 접두사 통계로 보정
"""

import csv
import html
import json
import pathlib
import re
import sys
from collections import Counter, defaultdict

BASE = pathlib.Path(__file__).parent
DATA = BASE / "data"
# 최초 146건(수집 경로 이전 원본). 이후 수집분은 전부 JSONL 로 관리한다.
BASE_CSV = DATA / "mcm-products-base.csv"
CSV_PATH = DATA / "mcm-products-full.csv"
DEFAULT_JSONL = [DATA / "mcm-crawl-raw.jsonl", DATA / "mcm-recrawl-146.jsonl"]
SITE = "https://kr.mcmworldwide.com"
IMG = "https://images.mcmworldwide.com/i/mcmworldwide/{pid}_{n}?sw=600"

FIELDS = [
    "pid", "name", "priceValue", "color", "gender", "group", "availability",
    "shortDesc", "longDesc", "material", "dimensions", "strap", "madeIn",
    "features", "sizes", "images", "url",
    # tag_product_mood.py 산출물. 재병합해도 유지되도록 기존 CSV 에서 승계한다.
    "mood",
]

# 상품군 판별 실패 시 상품명 키워드 폴백
NAME_HINT = [
    ("CLOTHING", ("티셔츠", "셔츠", "재킷", "자켓", "코트", "팬츠", "쇼츠", "스커트", "니트",
                  "스웨터", "후디", "후드", "가디건", "카디건", "블루종", "다운", "베스트",
                  "드레스", "원피스", "폴로", "스웨트", "스웻", "점퍼", "트랙", "수영", "블라우스")),
    ("SHOES", ("스니커즈", "슈즈", "샌들", "부츠", "로퍼", "슬라이드", "클로그", "펌프스", "뮬")),
    ("WALLET", ("지갑", "카드 케이스", "카드케이스", "카드홀더", "머니 클립")),
    ("ACCESSORY", ("벨트", "참", "스카프", "모자", "캡", "비니", "장갑", "키링", "키 링",
                   "폰케이스", "폰 케이스", "파우치", "브레이슬릿", "선글라스", "넥타이",
                   "워치", "밴드", "칼라", "리쉬")),
    ("BAG", ("백팩", "토트", "크로스바디", "숄더백", "백", "가방", "트롤리", "더플", "러기지")),
]


# 소재 표기는 PDP bullet 안에 섞여 있고 라벨이 제품군/언어마다 다르다.
# (가방 바디·트림 / 슈즈 어퍼·아웃솔 / 참 앞면·뒷면 / 영문 상품 Body·Trim …)
MATERIAL_LABEL = re.compile(
    r"^(바디|트림|소재|겉감|안감|앞면|뒷면|어퍼|아웃솔|인솔|라이닝|"
    r"body|trim|upper|outsole|insole|lining|material|fabric)\s*[::]",
    re.I,
)


# 라벨 없이 혼용률만 적는 표기도 흔하다("울 70%, 캐시미어 30%", "100% Organic Silk").
# 오탐을 막으려고 퍼센트 + 섬유/소재 키워드가 함께 있을 때만 소재로 본다.
FIBER = re.compile(
    r"코튼|면|울|캐시미어|실크|나일론|폴리|엘라스테인|엘라스틴|엘라스토디엔|레더|가죽|"
    r"스웨이드|비스코스|리넨|린넨|아크릴|모헤어|알파카|램스울|데님|ECONYL|"
    r"cotton|wool|silk|leather|nylon|polyester|polyamide|elastane|cashmere|viscose|"
    r"linen|acryl|suede|mohair|alpaca",
    re.I,
)
PERCENT = re.compile(r"\d{1,3}\s*%")


def is_material(item: str) -> bool:
    return bool(MATERIAL_LABEL.match(item)) or bool(PERCENT.search(item) and FIBER.search(item))


def split_material(material: str, features: str) -> tuple[str, str]:
    """features 안에 남아 있는 소재 항목을 material 쪽으로 옮긴다."""
    mats = [m for m in material.split(" | ") if m.strip()]
    rest = []
    for item in features.split(" | "):
        (mats if is_material(item.strip()) else rest).append(item)
    return " | ".join(dedupe(mats)), " | ".join(x for x in rest if x.strip())


def clean(text: str) -> str:
    """JSON-LD 원문에는 CMS 가 넣은 HTML 엔티티가 그대로 남아 있다(&reg; &rsquo; &uuml; …).
    DOM 에서 뽑은 필드는 이미 디코드돼 있지만 name·longDesc 는 그렇지 않아 여기서 처리한다."""
    return " ".join(html.unescape(text or "").split())


def dedupe(values):
    seen, out = set(), []
    for v in values:
        v = v.strip()
        if v and v not in seen:
            seen.add(v)
            out.append(v)
    return out


def infer_group(name: str) -> str:
    for group, words in NAME_HINT:
        if any(w in name for w in words):
            return group
    return "BAG"


def main() -> None:
    sources = [pathlib.Path(p) for p in sys.argv[1:]] or DEFAULT_JSONL
    raw = []
    for src in sources:
        raw += [
            json.loads(line)
            for line in src.read_text(encoding="utf-8").splitlines()
            if line.strip()
        ]

    # 이전 산출 CSV 의 mood 는 LLM 태깅 결과라 재병합으로 날리면 589건을 다시 태깅해야 한다.
    prior_mood: dict[str, str] = {}
    if CSV_PATH.exists():
        with CSV_PATH.open(encoding="utf-8-sig", newline="") as handle:
            for r in csv.DictReader(handle):
                if r.get("mood"):
                    prior_mood[r["pid"][:11]] = r["mood"]

    with BASE_CSV.open(encoding="utf-8-sig", newline="") as handle:
        old = list(csv.DictReader(handle))
    # 재수집분은 기존 행을 덮어쓴다(뒤에 오는 JSONL 이 우선). 스타일코드 11자리 기준.
    recrawled = {(r.get("base") or r["pid"][:11]) for r in raw}
    old = [r for r in old if r["pid"][:11] not in recrawled]

    # SKU 접두사(3자) → 상품군/성별 통계.
    # 캠페인 URL에만 노출되는 상품은 경로에서 분류를 못 얻으므로,
    # 기존 CSV + 이번 수집분에서 접두사별 최빈값을 학습해 보정한다.
    prefix = defaultdict(Counter)
    prefix_gender = defaultdict(Counter)
    for r in old + raw:
        pid = r.get("pid") or ""
        if not pid:
            continue
        if r.get("group"):
            prefix[pid[:3]][r["group"]] += 1
        if r.get("gender"):
            prefix_gender[pid[:3]][r["gender"]] += 1

    # 스타일코드 → 행. 같은 코드가 여러 번 나오면 나중 수집분이 이긴다(재수집 우선).
    rows: dict[str, dict] = {}
    dropped = Counter()
    for r in raw:
        base = r.get("base") or r["pid"][:11]
        if not r.get("name") or not r.get("priceValue"):
            dropped["empty"] += 1
            continue
        if base in rows:
            dropped["dup"] += 1

        group = r.get("group") or ""
        if not group:
            votes = prefix.get(r["pid"][:3])
            group = votes.most_common(1)[0][0] if votes else infer_group(r["name"])
            dropped["group_inferred"] += 1
        gender = r.get("gender") or ""
        if not gender:
            votes = prefix_gender.get(r["pid"][:3])
            gender = votes.most_common(1)[0][0] if votes else "WOMEN"
            dropped["gender_inferred"] += 1

        imgs = [n for n in (r.get("imgs") or "").split(",") if n]
        images = " | ".join(IMG.format(pid=r["pid"], n=n) for n in imgs)
        if not images:
            images = IMG.format(pid=r["pid"], n="01")

        material, features = split_material(r.get("material", ""), r.get("features", ""))

        rows[base] = {
            "pid": r["pid"],
            "name": clean(r["name"]),
            "priceValue": int(r["priceValue"]),
            "color": r.get("color", ""),
            "gender": gender,
            "group": group,
            "availability": r.get("availability", "InStock"),
            "shortDesc": clean(r.get("shortDesc", "")),
            "longDesc": clean(r.get("longDesc", "")) or clean(r.get("shortDesc", "")),
            "material": clean(material),
            "dimensions": r.get("dimensions", ""),
            "strap": r.get("strap", ""),
            "madeIn": r.get("madeIn", ""),
            "features": clean(features),
            "sizes": " | ".join(dedupe((r.get("sizes") or "").split("|"))),
            "images": images,
            "url": SITE + r["path"] if r.get("path") else "",
            "mood": prior_mood.get(base, ""),
        }

    # 기존 행도 소재/특징 분리 규칙을 동일하게 적용한다(초기 수집분은 라벨 있는
    # 소재만 잡혀 있어 "울 70%, 캐시미어 30%" 같은 표기가 features 에 남아 있다).
    for r in old:
        r["material"], r["features"] = split_material(r.get("material", ""), r.get("features", ""))
        for field in ("name", "shortDesc", "longDesc", "material", "features"):
            r[field] = clean(r.get(field, ""))
        r["mood"] = prior_mood.get(r["pid"][:11], "")

    merged = old + list(rows.values())
    with CSV_PATH.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDS, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(merged)

    counts = Counter((r["gender"], r["group"]) for r in merged)
    print(f"기존 {len(old)} + 신규 {len(rows)} = {len(merged)}건 → {CSV_PATH}")
    print(f"스킵: {dict(dropped)}")
    for key in sorted(counts):
        print(f"  {key[0]:<6} {key[1]:<10} {counts[key]}")


if __name__ == "__main__":
    main()
