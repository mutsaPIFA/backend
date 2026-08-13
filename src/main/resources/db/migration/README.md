# Flyway 마이그레이션

스키마는 **Flyway가 소유**한다 (`spring.jpa.hibernate.ddl-auto=validate`).
Hibernate가 테이블을 만들지 않으므로, 엔티티를 추가하면 반드시 여기에 마이그레이션도 추가해야 앱이 뜬다.

## 현재 버전

| 파일 | 내용 |
|------|------|
| `V1__init.sql` | 전체 테이블 7개 — ERD·설계 근거는 [`../../../../docs/erd.md`](../../../../docs/erd.md) |
| `V2__seed_moods.sql` | 무드 고정 시드 6개 (계약 `api-v1.md` §4-3, Figma 정렬 확정 시 V3로 UPDATE) |

MCM 상품 시드(150~200개)는 SQL이 아니라 `resources/seed/mcm-products.json` + 적재 파이프라인으로 넣는다
(이미지 fetch + rembg로 `cutoutUrl` 생성이 필요해서 SQL로 처리할 수 없음).

## 규칙

- 파일명 `V{번호}__{설명}.sql`, 번호는 순차 증가
- **이미 머지된 마이그레이션은 수정 금지** — 새 버전을 추가한다 (체크섬 불일치로 앱이 안 뜸)
