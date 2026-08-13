# DB 컨벤션

> 스키마 자체는 [`../erd.md`](../erd.md)와 Flyway 마이그레이션이 기준. 여기는 **테이블·엔티티를 만들 때 지키는 규칙**.

## 스키마 소유권

- **스키마는 Flyway가 소유한다.** `ddl-auto=validate` — Hibernate는 테이블을 만들지 않는다.
- 엔티티를 추가·변경하면 **반드시 마이그레이션도 추가**해야 앱이 뜬다.
- 이미 머지된 마이그레이션은 수정 금지 — 새 버전(`V{n}`)을 추가한다. 시드 데이터 수정도 마찬가지(`V2` 무드를 고치고 싶으면 `V3`로 UPDATE).

## 네이밍

- 테이블: `snake_case` 복수형 (`closet_items`, `look_closet_items`)
- 컬럼: `snake_case` (`mcm_product_id`, `created_at`)
- 제약: `uq_{table}_{col}` · 인덱스: `idx_{table}_{용도}`
- Java 엔티티 ↔ 테이블 매핑은 Hibernate 기본 네이밍 전략에 맡긴다 (camelCase → snake_case 자동)

## 타입

- **ID: `BIGSERIAL` / Java `Long`** — 전 테이블 통일 (tech-blog ADR-0002 준용)
- **시각: `TIMESTAMPTZ`** (UTC), 날짜만 필요하면 `DATE` (`worn_date`)
- **금액: `INTEGER`** — KRW 원 단위, 소수 없음
- **URL: `TEXT`** — 길이 제한 없음
- controlled vocabulary(category/color/material/mood): **`VARCHAR` + Java `@Enumerated(EnumType.STRING)`** — DB enum·코드 테이블 안 씀. 값 추가는 enum 상수 + 계약 문서 갱신으로 끝. 유효값의 단일 소스는 계약(`docs/api-v1.md` 공통 타입)

## 공통 컬럼

- 모든 도메인 테이블에 `created_at`, `updated_at` (`TIMESTAMPTZ NOT NULL DEFAULT now()`) — JPA Auditing으로 채운다
- 예외: 조인 전용(`look_closet_items`)·고정 시드(`moods`)·`refresh_tokens`(created_at만)

## 소프트 삭제 (closet_items)

- 삭제 = `deleted_at = now()`. **row는 지우지 않는다** — 저장된 룩(`look_closet_items` FK)이 참조한다.
- **모든 활성 조회에 `deleted_at IS NULL` 필터.** 빠뜨리면 삭제된 옷이 되살아나는 버그다.
  → repository **기본 메서드에 조건을 내장**하고(`findByUserIdAndDeletedAtIsNull...`), 필터 없는 조회는 룩 기록 조회 등 의도된 곳에만 명시적으로 둔다.
- 룩 조회는 삭제된 아이템도 계속 보여준다 (과거 코디 기록 보존 — 계약 §4-6/4-7 참고)

## FK · 삭제 정책

- 참조는 **항상 FK 제약**을 건다. "나중에 정리하지"로 무결성을 앱에 맡기지 않는다.
- `ON DELETE CASCADE`는 **소유 관계**(users → 하위 데이터, looks → look_closet_items)에만.
- 참조 관계(`closet_items → mcm_products`, `looks → mcm_products`)는 CASCADE 금지 — 상품이 사라져도 옷장·기록은 남아야 한다.

## MCM 상품 재적재

- 시드·주최측 피드 적재는 **`sku` UNIQUE 키로 upsert** — 있으면 갱신, 없으면 삽입. **전체 삭제 후 재삽입 금지** (옷장·룩 FK가 깨진다).
- 피드에서 사라진 상품은 `active=false` — 카탈로그 조회에서만 숨는다. **row 삭제 금지.**
- 카탈로그 조회(`GET /mcm-products`)는 항상 `active=true` 필터. 이미 옷장에 담긴 비활성 상품은 옷장·룩에서 계속 보인다.

## Refresh Token

- DB 저장(`refresh_tokens`) + **회전**: 갱신마다 기존 row 삭제 → 새 row 삽입. 로그아웃 = 해당 유저 row 삭제.
- **원문 저장 금지** — SHA-256 해시(`token_hash`)만 저장한다.
- 만료 row는 조회 시 무시하고, 정리는 필요해질 때 배치로.
