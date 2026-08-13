"""MCM 상품 CSV → 시드 JSON 정규화 (수동 실행 — 재수확 시 재실행)

사용법:
    python scripts/csv_to_seed.py [CSV경로]
    (기본 CSV: scripts/data/mcm-products-full.csv → 출력: src/main/resources/seed/mcm-products.json)

정규화 규칙 (계약 controlled vocabulary 기준, 2026-08-13 검수 완료):
- category: BAG→가방, WALLET/ACCESSORY→악세서리, SHOES→신발,
            CLOTHING→상품명 키워드(원피스>아우터>하의>상의 우선순위, 미분류 시 상의 폴백)
- color:    영문 키워드 매핑, 공란·미매핑(콜라보명 등)→기타
            ※ 기타 67건은 추후 무료 태깅으로 보강 가능 (my/프롬프트 정의.md)
- material: 소재 텍스트+상품명 키워드, 우선순위 실크>울>데님>니트>가죽>합성>면
- OutOfStock 포함 전부 적재 (active=true — 구매는 공식몰 딥링크라 재고는 저쪽 소관)
"""

import csv
import io
import json
import pathlib
import re
import sys

BASE = pathlib.Path(__file__).parent
DEFAULT_CSV = BASE / "data" / "mcm-products-full.csv"
OUT = BASE.parent / "src" / "main" / "resources" / "seed" / "mcm-products.json"

GROUP = {"BAG": "가방", "WALLET": "악세서리", "ACCESSORY": "악세서리", "SHOES": "신발"}
DRESS = ["드레스", "원피스"]
OUTER = ["재킷", "자켓", "코트", "점퍼", "블루종", "파카", "베스트", "가디건", "아노락", "윈드브레이커", "다운"]
BOTTOM = ["팬츠", "바지", "쇼츠", "스커트", "치마", "조거", "버뮤다", "트라우저"]
TOP = ["티셔츠", "셔츠", "니트", "스웨터", "후드", "후디", "맨투맨", "블라우스", "탑", "폴로", "저지", "스웻셔츠"]

COLOR = [
    ("블랙", r"Black"), ("화이트", r"White|Ivory|Cream"), ("네이비", r"Navy|Deep Blue"),
    ("그레이", r"Gray|Grey|Charcoal"), ("베이지", r"Beige|Sand|Oat"),
    ("브라운", r"Brown|Chocolate|Cinnamon|Mocha"), ("카멜", r"Cognac|Camel|Tan|Caramel"),
    ("그린", r"Green|Khaki|Olive|Moss"), ("핑크", r"Pink|Rose|Blush"),
]
MATERIAL = [
    ("실크", r"실크|Silk"), ("울", r"울|캐시미어|Wool|Cashmere|모헤어"), ("데님", r"데님|Denim"),
    ("니트", r"니트"), ("가죽", r"카프스킨|램스킨|가죽|레더|Leather|스킨"),
    ("합성", r"비세토스|코팅|나일론|폴리|Poly|Nylon|리사이클"), ("면", r"코튼|면|Cotton|캔버스"),
]


def clothing_category(name: str) -> str:
    for kws, cat in ((DRESS, "원피스"), (OUTER, "아우터"), (BOTTOM, "하의"), (TOP, "상의")):
        if any(kw in name for kw in kws):
            return cat
    return "상의"  # 폴백 (2026-08-13 기준 미분류 0건)


def map_first(text: str, table, default: str) -> str:
    for ko, pat in table:
        if text and re.search(pat, text, re.I):
            return ko
    return default


def main() -> None:
    csv_path = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_CSV
    rows = list(csv.DictReader(io.open(csv_path, encoding="utf-8-sig")))
    out = []
    for r in rows:
        category = GROUP.get(r["group"]) or clothing_category(r["name"])
        out.append(
            {
                "sku": r["pid"],
                "name": r["name"],
                "category": category,
                "color": map_first(r["color"], COLOR, "기타"),
                "material": map_first((r["material"] or "") + " " + r["name"], MATERIAL, "기타"),
                "price": int(r["priceValue"]),
                "imageUrl": r["images"].split("|")[0].strip() if r["images"] else "",
                "productUrl": r["url"].strip(),
            }
        )
    OUT.parent.mkdir(parents=True, exist_ok=True)
    io.open(OUT, "w", encoding="utf-8", newline="\n").write(
        json.dumps(out, ensure_ascii=False, indent=1)
    )
    print(f"{len(out)}건 → {OUT}")


if __name__ == "__main__":
    main()
