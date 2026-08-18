"""MCM 상품 CSV에 mood 라벨을 채운다 (오프라인 배치 — 수동 실행).

옷장 아이템은 사진으로 무드를 태깅하지만(AI/main.py `/vision/tag`), 상품은 이미
상품명·요약설명·특징 텍스트가 무드를 서술하고 있어 텍스트로 태깅한다. 589장을
vision 으로 돌릴 이유가 없다.

⚠️ 무드 판단 기준 문장은 AI/main.py TAG_PROMPT 4번과 동일하게 유지한다. 옷장(vision)과
상품(text)의 경로가 갈리므로 기준이 두 벌이 되면 매칭이 조용히 어긋난다. TAG_PROMPT 를
고치면 여기도 같이 고칠 것.

여기에만 있는 조항: "브랜드 고급스러움으로 럭셔리를 고르지 말 것". 입력이 전부 한 럭셔리
브랜드라 그대로 태깅하면 럭셔리가 50%(296/589 실측)로 쏠려 mood 가 변별력을 잃는다.
무드의 정의를 바꾸는 조항이 아니라 입력 도메인 편향을 걷어내는 조항이다.

사용법:
    python scripts/tag_product_mood.py            # mood 가 빈 행만 태깅
    python scripts/tag_product_mood.py --all      # 전부 다시 태깅
    python scripts/tag_product_mood.py --limit 40 # 앞 40건만 (검수용)

결과는 CSV 의 mood 컬럼에 기록하고, 이어서 csv_to_seed.py 를 돌리면 시드에 반영된다.
중간 실패해도 배치 단위로 CSV 에 저장하므로 다시 실행하면 남은 건만 이어서 태깅한다.
"""

import argparse
import csv
import io
import json
import os
import pathlib
import sys
import time

from dotenv import load_dotenv
from google import genai
from google.genai import errors as genai_errors
from google.genai import types

BASE = pathlib.Path(__file__).parent
CSV_PATH = BASE / "data" / "mcm-products-full.csv"
ENV_PATH = BASE.parent.parent / "AI" / ".env"

MOODS = ["미니멀", "캐주얼", "클래식", "스트릿", "페미닌", "럭셔리"]
BATCH = 25

PROMPT = """당신은 패션 아이템의 분위기를 분류합니다.

아래 MCM 제품 목록 각각에 대해 mood 를 하나씩 판단하세요.

[mood 후보]
미니멀 | 캐주얼 | 클래식 | 스트릿 | 페미닌 | 럭셔리

[판단 기준]
- 색상 하나만으로 판단하지 말고 제품의 실루엣, 디자인, 소재, 패턴 등을 종합적으로 고려하여
  가장 적합한 분위기 하나를 선택하세요.
- 입력 제품은 전부 같은 럭셔리 브랜드(MCM) 제품입니다. 따라서 브랜드의 가격대, 고급 소재,
  24K 도금 하드웨어 같은 마감 수준은 제품 간 변별 요소가 아닙니다. 이런 이유로 "럭셔리"를
  고르지 마세요.
- "럭셔리"는 실루엣과 디자인 자체가 장식적이거나 포멀·드레시한 제품에만 쓰세요.
  같은 브랜드 안에서 상대적으로 어떤 스타일인지를 기준으로 6개 후보에 고르게 분류하세요.
- 반드시 제공된 후보 중에서만 값을 선택하세요.
- 입력에 없는 정보를 임의로 추측하지 마세요.

입력 목록의 순서와 개수를 그대로 유지해 각 항목의 id 와 mood 를 출력하세요."""

SCHEMA = types.Schema(
    type=types.Type.OBJECT,
    properties={
        "items": types.Schema(
            type=types.Type.ARRAY,
            items=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "id": types.Schema(type=types.Type.STRING),
                    "mood": types.Schema(type=types.Type.STRING, enum=MOODS),
                },
                required=["id", "mood"],
            ),
        )
    },
    required=["items"],
)


def brief(row: dict) -> dict:
    """무드 판단에 쓰는 최소 텍스트 — 가격·치수 등 무관한 필드는 넣지 않는다."""
    return {
        "id": row["pid"],
        "name": row["name"],
        "category": row["group"],
        "color": row["color"],
        "summary": row["shortDesc"],
        "features": " / ".join(row["features"].split(" | ")[:5]),
    }


def tag_batch(client: genai.Client, model: str, rows: list[dict]) -> dict[str, str]:
    payload = [brief(r) for r in rows]
    resp = client.models.generate_content(
        model=model,
        contents=[PROMPT, json.dumps(payload, ensure_ascii=False)],
        config=types.GenerateContentConfig(
            response_mime_type="application/json", response_schema=SCHEMA
        ),
    )
    out = {}
    for item in json.loads(resp.text).get("items", []):
        if item.get("mood") in MOODS:
            out[item["id"]] = item["mood"]
    return out


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--all", action="store_true", help="mood 가 이미 있어도 다시 태깅")
    parser.add_argument("--limit", type=int, default=0, help="앞 N건만 (검수용)")
    args = parser.parse_args()

    load_dotenv(ENV_PATH)
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        raise SystemExit(f"GEMINI_API_KEY 없음 ({ENV_PATH})")
    model = os.environ.get("GEMINI_TAG_MODEL", "gemini-2.5-flash")
    client = genai.Client(api_key=api_key)

    rows = list(csv.DictReader(io.open(CSV_PATH, encoding="utf-8-sig")))
    fields = list(rows[0].keys())
    if "mood" not in fields:
        fields.append("mood")
    for r in rows:
        r.setdefault("mood", "")

    todo = [r for r in rows if args.all or not (r.get("mood") or "").strip()]
    if args.limit:
        todo = todo[: args.limit]
    print(f"대상 {len(todo)}건 / 전체 {len(rows)}건, 배치 {BATCH}, 모델 {model}")

    tagged = 0
    for start in range(0, len(todo), BATCH):
        chunk = todo[start : start + BATCH]
        try:
            result = tag_batch(client, model, chunk)
        except (genai_errors.APIError, json.JSONDecodeError, ValueError) as error:
            print(f"  배치 {start // BATCH + 1} 실패: {error}", file=sys.stderr)
            continue
        for r in chunk:
            if r["pid"] in result:
                r["mood"] = result[r["pid"]]
                tagged += 1
        # 배치마다 저장 — 중간에 죽어도 이어서 실행 가능
        with io.open(CSV_PATH, "w", encoding="utf-8-sig", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore")
            writer.writeheader()
            writer.writerows(rows)
        print(f"  {start + len(chunk)}/{len(todo)} (누적 {tagged}건)")
        time.sleep(1)

    missing = sum(1 for r in rows if not (r.get("mood") or "").strip())
    print(f"태깅 {tagged}건 완료, 미태깅 잔여 {missing}건 → {CSV_PATH}")


if __name__ == "__main__":
    main()
