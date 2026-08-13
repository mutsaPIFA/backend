# MCM MUSE — backend

멋쟁이사자처럼 중앙해커톤 Challenge 03 · **MCM MUSE** AI 스타일링 앱의 백엔드.
"내 옷장에서 시작해 MCM을 더하고 다시 옷장으로 스타일링하는 한 바퀴."

## 스택

- Java 21 · **Spring Boot 3.5.0** · Gradle 8.14.4
- PostgreSQL 16 · Flyway · Spring Data JPA
- Spring Security + JWT (access token + refresh 쿠키)
- Docker / docker-compose

## 로컬 실행

```bash
cp .env.example .env      # JWT_SECRET 채우기: openssl rand -base64 48
docker compose up -d postgres
./gradlew bootRun         # 또는 IntelliJ에서 McmMuseApplication Run

curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

> ⚠️ **postgres는 호스트 `5433`으로 노출된다.** 로컬에 PostgreSQL이 설치돼 서비스로 돌고 있으면 5432를 점유해서, 앱이 컨테이너가 아니라 로컬 PG로 붙어 인증 실패한다. 컨테이너 내부 통신은 그대로 `postgres:5432`.

전체 통합 실행(백엔드까지 컨테이너로):

```bash
docker compose up --build
docker compose --profile ai up --build   # ai 레포까지 함께
```

## 패키지 구조

도메인 경계(Bounded Context) 4개 + 전역 공용:

```
com.mutsapifa.mcmmuse
├─ auth/      인증·계정          → API 정의서 §1
├─ catalog/   MCM 상품 카탈로그   → §2
├─ closet/    내 옷장·스캔        → §3
├─ styling/   DNA·추천·코디·룩    → §4
└─ shared/    config · exception · storage · aiclient
```

각 BC는 `domain / application / infrastructure / presentation` 4계층.
→ 상세: [`docs/conventions/패키지-구조.md`](docs/conventions/패키지-구조.md)

## 구조 (멀티 레포)

| 레포 | 역할 |
| --- | --- |
| `backend` (여기) | 공개 `/api/v1` · DB · 통합 허브 · 루트 docker-compose |
| `frontend` | 앱 UI (React) |
| `ai` | Python FastAPI — rembg(누끼) + Gemini(태깅·추천·이미지 생성) |

## 문서

**팀 공용** (→ [`mutsaPIFA/docs`](https://github.com/mutsaPIFA/docs))

- [**API 정의서 v1**](https://github.com/mutsaPIFA/docs/blob/main/api-v1.md) — **엔드포인트의 최종 기준.** 요청/응답·필드·에러·화면 배지
- [API 계약·결정 배경](https://github.com/mutsaPIFA/docs/blob/main/api-contract-v1.md) — 데이터모델·결정로그·화면맵
- [아키텍처 개요](https://github.com/mutsaPIFA/docs/blob/main/architecture.md) — 시스템 큰 그림
- [비전/LLM 리서치](https://github.com/mutsaPIFA/docs/blob/main/research-vision-llm.md)

**백엔드 전용** (여기)

| 문서 | 성격 |
| --- | --- |
| [`docs/conventions/패키지-구조.md`](docs/conventions/패키지-구조.md) | 현재 규칙 — BC·계층 의존·파일 위치 |
| [`docs/specs/2026-08-13-backend-skeleton-design.md`](docs/specs/2026-08-13-backend-skeleton-design.md) | 시점 기록 — 뼈대 설계·결정 근거 |

> `conventions/`는 계속 유지되는 규칙, `specs/`는 그 시점의 설계 기록. 규칙이 바뀌면 `conventions/`를 고친다.

## 협업

- 브랜치: `main`(최종 데모) / `dev`(통합) / `feature/*`(작업). **PR base는 `dev`**, `main` 직접 푸시 금지.
- **API 계약 변경은 `docs` 레포 PR로.** 영향받는 역할(프론트/AI)을 리뷰어로 지정한다.

## 현재 상태

✅ **뼈대 세팅 완료** — Gradle/Spring Boot 스캐폴딩, BC 패키지 구조, docker-compose, 기동 확인(`/actuator/health` UP).

다음 단계:
- [ ] **ERD·스키마 확정** → `docs/` 에 문서화 후 Flyway `V1__init.sql`
- [ ] `shared` (SecurityConfig·전역 예외·JWT) + `auth` 실제 동작
- [ ] MCM 상품 시드 150~200개 적재 파이프라인
- [ ] 나머지 BC — 빌드 순서는 설계 문서 §12
