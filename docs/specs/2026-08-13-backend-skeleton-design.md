# MCM MUSE — 백엔드 뼈대 설계 (skeleton design)

- 작성일: 2026-08-13
- 대상: `mutsaPIFA/backend` (+ 신규 `mutsaPIFA/ai`)
- 성격: **백엔드 구현 상세** (팀 공용 문서 아님). 큰 그림은 팀 공용 [`docs/architecture.md`](https://github.com/mutsaPIFA/docs/blob/main/architecture.md) 참조.
- 전제 계약: **엔드포인트는 [`docs/api-v1.md`](https://github.com/mutsaPIFA/docs/blob/main/api-v1.md)가 단일 기준** · 결정 배경은 [`docs/api-contract-v1.md`](https://github.com/mutsaPIFA/docs/blob/main/api-contract-v1.md) · [`docs/research-vision-llm.md`](https://github.com/mutsaPIFA/docs/blob/main/research-vision-llm.md)
- 목표(토요일 = 2026-08-15): 계약 JSON 모양대로 응답하는 **영속 계층 포함 백엔드** + **분리된 AI 서비스** 뼈대

---

## 1. 서비스 토폴로지 (멀티 레포 + docker-compose)

```
C:\pj\fifa\
├─ docs\      → mutsaPIFA/docs       팀 공용 문서(계약·아키텍처·리서치)
├─ backend\   → mutsaPIFA/backend    Spring Boot · DB · 공개 /api/v1 · 통합 허브 · 루트 compose
├─ frontend\  → mutsaPIFA/frontend   (프론트 담당)
└─ ai\        → mutsaPIFA/ai         Python FastAPI · rembg + Gemini (AI 담당 시은)
   (루트 docker-compose.yml 은 backend 레포에 포함)
```

**왜 AI를 분리?** rembg(누끼)가 파이썬 네이티브·Gemini 파이썬 SDK 1급 → 파이썬이 최단경로. 오너십 분리(시은), 이미지 생성(~20s) 독립 스케일, `docker-compose up` 으로 로컬 통합·최종 배포 일원화.

**협업:** PR 머지 정책. `main` 직접 푸시 지양, feature 브랜치 → PR → 머지.

---

## 2. 백엔드 스택

| 항목 | 결정 |
|---|---|
| 언어/런타임 | Java 21 (Temurin) |
| 프레임워크 | **Spring Boot 3.5.0** (초안의 3.3.x에서 상향 — §9 참조) |
| 빌드 | Gradle (Groovy DSL) + wrapper |
| DB | PostgreSQL 16 |
| 마이그레이션 | Flyway |
| 인증 | Spring Security + JWT (jjwt) |
| 테스트 DB | Testcontainers-postgres (운영과 동일 방언, Docker 사용) |
| base package | `com.mutsapifa.mcmmuse` |

---

## 3. 패키지 구조 (Bounded Context + DDD-Lite 4계층)

> 🔄 **2026-08-13 변경**: `package-by-feature` 9개(auth/closet/product/styledna/recommend/curation/storage/aiclient/common) → **BC 4개 + shared**.
> 이유: feature 9개는 화면·엔드포인트 단위라 잘게 쪼개져 공유 로직 자리가 애매했고, `product`처럼 화면 이름에 가까운 것도 있었다. tech-blog에서 쓰던 DDD-Lite 컨벤션에 맞춰 도메인 경계로 재정렬. 팀 결정 로그 D12 참조.

```
com.mutsapifa.mcmmuse
├─ McmMuseApplication
│
├─ auth/                    # User · 로그인/회원가입/토큰 · GET /me
├─ catalog/                 # McmProduct · 시드 적재 · 검색/상세     (← 옛 product)
├─ closet/                  # ClosetItem · 스캔 · 등록/조회/삭제
├─ styling/                 # Mood · Look · StyleDna · Recommendation · Outfit
│                           #   (← 옛 styledna + recommend + curation)
└─ shared/                  # BC 아님 — 모든 BC가 의존, 어떤 BC도 import 안 함
    ├─ config/              #   SecurityConfig, CORS, JwtAuthFilter
    ├─ exception/           #   GlobalExceptionHandler, ApiError
    ├─ storage/             #   StorageService(interface) + Local/S3, GET /images/{key}
    └─ aiclient/            #   AI 서비스 HTTP 클라이언트 + Mock 구현
```

### 왜 이렇게 나눴나 (결정 근거)

- **`home` → `catalog`** — `home`은 화면 이름이지 도메인 이름이 아니다. 이 프로젝트는 이미 화면 번호를 통째로 갈아엎은 적이 있어서, 패키지를 화면에 묶으면 이름이 곧 거짓말이 된다. 그 자리에 있는 모델은 `McmProduct`다.
- **추천 → `closet`이 아니라 `styling`** — `POST /style-dna`·`POST /recommendations`는 입력만 옷장이고 출력은 MCM 제품 추천이다. `closet`에 두면 `ClosetItem`과 `Recommendation`이 한 BC에 섞인다.
- **`profile` BC 안 만듦** — 프로필에 담을 게 닉네임뿐이라 엔티티가 0개다. 빈 껍데기 대신 `GET /me`를 `auth`에 둔다. 프로필 이미지·취향 설정이 붙으면 그때 분리.
- **`styling`이 다소 무거운 것은 감수** — 추천과 큐레이터를 더 쪼갤 수도 있지만, 둘 다 "옷장을 읽어 MCM을 제안한다"라 경계가 애매하고 공유 로직이 생길 가능성이 높다.

> 📐 **계층 구조·의존 방향·파일 위치 가이드는 [`../conventions/패키지-구조.md`](../conventions/패키지-구조.md)** 에 있다.
> 이 문서는 *2026-08-13 시점의 설계 기록*이고, 저쪽이 *계속 유지되는 규칙*이다. 규칙이 바뀌면 저 문서를 고친다.

---

## 4. 영속 계층 (실제 구현)

**JPA 엔티티 (영속):** `User`, `ClosetItem`, `McmProduct`, `Mood`, `Look`
- `ClosetItem { id, user, category, color, material, mood, imageUrl, cutoutUrl, source(OWN|MCM enum), mcmProductId(nullable), createdAt }`
  - 조회 정렬은 **`createdAt DESC` 고정** (계약 D10).
- `McmProduct { id, name, category, color, material, price, imageUrl, cutoutUrl(nullable), productUrl }`
  - **`cutoutUrl` 추가(D4)** — 화면 16 콜라주에서 MCM만 흰 배경으로 튀는 문제. 시드 적재 시 rembg로 생성.
  - `productUrl` = MCM 공식몰 PDP. 화면 6 "구매하기"가 이 URL로 외부 이동(우리가 결제 처리 안 함, D6).
- `Look { id, user, wornDate, moodId, occasionLabel, closetItemIds(@ElementCollection), mcmProductId, reason, generatedImageUrl(nullable) }`
  - `wornDate` 미전송 시 **서버가 오늘 날짜로 채움**(D9).
  - `generatedImageUrl`은 **비동기 생성**(D2) — `POST /looks`는 즉시 201(+null) 반환하고 백그라운드에서 채운다. 프론트는 `GET /looks/{id}` 폴링.
- `Mood` 는 시드 6개(계약 표), `McmProduct` 는 **시드 카탈로그 150~200개**(아래 §13).

**transient (미저장, 계약 원칙 Q9):** `Recommendation`, `Outfit`, `StyleDna` → 엔티티 아님, DTO로만 존재. 매 호출 생성.

**마이그레이션:** Flyway `V1__init.sql`(테이블) + `V2__seed_moods.sql`(무드 6개). McmProduct 시드는 별도.

**controlled vocabulary**(category/color/material/mood)는 계약 §3 enum. v1은 문자열 컬럼 + 애플리케이션 검증(추후 DB enum/코드테이블로 승격 가능).

---

## 5. AI 연동 (id 재검증으로 환각 차단)

백엔드는 AI 응답을 **그대로 신뢰하지 않음** — 반환된 `mcmProductId`/`closetItemId` 를 **DB로 재조회 후 실재하는 것만** 응답에 실음 (계약 원칙 2).

**포트 인터페이스** (`shared/aiclient`), 각각 2개 구현:
| 인터페이스 | 역할 | 구현 A (기본) | 구현 B |
|---|---|---|---|
| `VisionTagger` | 사진→태그 | **Mock**(고정 태그) | HTTP→ai `/vision/tag` |
| `BackgroundRemover` | 누끼 | **Mock**(원본=누끼) | HTTP→ai `/cutout` |
| `Recommender` | DNA·추천 | **Mock**(DB에서 샘플) | HTTP→ai `/recommend`,`/style-dna` |
| `OutfitComposer` | 코디 3개 | **Mock**(규칙 조합) | HTTP→ai `/outfits` |
| `LookImageGenerator` | B 이미지 | **Mock**(null) | HTTP→ai `/looks/image` |

프로필/설정(`ai.mode=mock|http`)으로 스위치 → **ai 서비스·Gemini 키 없이도 백엔드·프론트 개발 진행**. Gemini 키 확정(시은)되면 http 모드로 전환.

---

## 6. AI 서비스 (FastAPI) — 내부 API 초안

공개 `/api/v1` 과 **별개**. backend만 호출(내부망). 계약 상세는 ai 레포에서 확정.
```
POST /vision/tag     (image)              → {category,color,material,mood}
POST /cutout         (image)              → {cutoutImage | url}         # rembg
POST /style-dna      {items:[...]}        → {summary,dominantColors,dominantMoods,keywords}
POST /recommend      {items:[...]}        → {bestPick, more:[...]}      # 후보 id 제안(백엔드가 재검증)
POST /outfits        {moodId, closet, seed?} → [ {closetItemIds, mcmProductId, reason}, ... ]
POST /looks/image    {person?, items, mcm} → {imageUrl}                 # Nano Banana, 저장 시 1장만
```
스택: FastAPI + uvicorn, `rembg`, `google-genai`(Gemini). Dockerfile 포함.

---

## 7. 이미지 저장 (계약 §5)

`StorageService` 인터페이스: `store(bytes, ext)→key`, `resolveUrl(key)`, `delete(key)`.
- 데모: `LocalStorageService`(디스크 저장 + `GET /images/{key}` 정적 서빙).
- 프로덕션: `S3StorageService`(무통증 교체). DB엔 URL/key만.

---

## 8. 공통·인증·에러

- **인증:** JWT **access token(body) + refresh token(HttpOnly 쿠키)**. `/auth/**`, `/images/**`, actuator health 공개. 나머지 `Authorization: Bearer`.
  - 쿠키를 쓰므로 **CORS에 `allowCredentials=true`** 필요(와일드카드 오리진 불가 — 명시적 오리진 목록).
- **에러:** `@RestControllerAdvice` → **`{ "status": 409, "message": "...", "code": "NO_MCM_IN_CLOSET" }`**.
  - `status`(HTTP 코드)·`message`(사용자 노출 가능한 한국어)는 항상, `code`는 **프론트가 분기해야 하는 비즈니스 에러에만** 부여.
  - v1 비즈니스 `code`는 `NO_MCM_IN_CLOSET` 하나뿐. 나머지는 HTTP status로만 구분.
- **CORS:** 프론트 로컬 오리진 허용(`app.cors.allowed-origins` 설정값).
- **SecurityConfig 작성 시 permitAll 할 경로** — 지금은 Security 기본값이 전부 막고 있어 Swagger도 `401`이 난다:
  `/auth/**` · `/images/**` · `/actuator/health` · `/swagger-ui/**` · `/v3/api-docs/**`

> ⚠️ 이 문서 예전 판에는 에러가 `{code, message}`로 적혀 있었으나 계약은 `{status, message, code?}`다. 계약(`api-v1.md` 공통 응답/에러)이 기준.

---

## 9. 설정 · 도커

- `application.yml`(공통) + `application-local.yml`(로컬 DB·로깅). 프로필 기본값 `local`.
- **backend/docker-compose.yml**(루트 오케스트레이션): `postgres`, `backend`(build .), `ai`(build ../ai). `.env` 로 시크릿(`.env.example` 복사해서 사용).
- backend `Dockerfile`(멀티스테이지 Gradle build → JRE 21 런타임).
- Gradle wrapper **8.14.4**, Spring Boot **3.5.0**, Java 21.
  - Initializr 기본은 이제 Boot 4.1.x지만, 메이저 변경(Spring Framework 7)이라 이틀짜리 일정에 리스크가 커서 tech-blog와 같은 **3.5.0**으로 맞췄다.

> ⚠️ **postgres는 호스트 5433으로 노출한다.** 로컬에 PostgreSQL이 설치돼 서비스로 돌면 5432를 점유해서, 앱이 컨테이너가 아니라 로컬 PG로 붙어 인증 실패한다(실제로 겪음 — `postgresql-x64-17`).
> 컨테이너 내부 통신은 그대로 `postgres:5432`, 호스트/IntelliJ에서만 `localhost:5433`.

### 로컬 실행 (IntelliJ)

```bash
cp .env.example .env          # JWT_SECRET 채우기: openssl rand -base64 48
docker compose up -d postgres # DB만 (백엔드는 IntelliJ에서 Run)
./gradlew bootRun             # 또는 IntelliJ에서 McmMuseApplication Run
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

**검증 완료(2026-08-13):** `compileJava` 통과, Postgres 16 연결·Flyway 초기화·Tomcat 기동까지 확인.

---

## 10. 테스트 (Testcontainers-postgres)

영속 계층이므로 실제 DB 대상 테스트:
- repository 슬라이스: ClosetItem/Look CRUD, 시드 로딩.
- controller 통합: 인증 플로우, `/closet-items` 등록/조회/삭제, `/outfits` NO_MCM_IN_CLOSET 케이스.
- AI 포트는 Mock 구현으로 대체(결정론적).

---

## 11. 엔드포인트 → 컨트롤러 매핑 (계약 §4 전체)

| 계약 엔드포인트 | BC | 영속? |
|---|---|---|
| POST /auth/register · /auth/login · /auth/refresh · /auth/logout | `auth` | ✅ User |
| **GET /me** | `auth` | ✅ User |
| GET /mcm-products[/{id}] | `catalog` | ✅ McmProduct |
| POST /scan | `closet` | 임시(미저장) |
| POST /closet-items (scan/mcm) · GET · DELETE | `closet` | ✅ ClosetItem |
| POST /style-dna | `styling` | ❌ transient |
| POST /recommendations | `styling` | ❌ transient |
| GET /moods | `styling` | ✅ Mood(시드) |
| POST /outfits | `styling` | ❌ transient |
| POST /looks | `styling` | ✅ Look (+B 이미지 **비동기** 생성 훅) |
| **GET /looks/{id}** | `styling` | ✅ Look (이미지 생성 폴링용) |
| GET /looks | `styling` | ✅ Look |

---

## 12. 빌드 순서 (walking skeleton first)

1. Gradle/SB 부트스트랩 + docker-compose(postgres) + `application.yml` + health 확인
2. `shared`(에러/보안/JWT/CORS) + `auth`(register·login·refresh·logout·me) — 실제 동작
3. 영속: 엔티티·repo·Flyway·시드(Mood 6개)
4. `catalog`(McmProduct + 시드 적재 파이프라인) — 다른 BC가 참조하므로 먼저
5. `closet`(scan[Mock AI]·closet-items[영속])
6. `styling`(style-dna·recommendations·moods·outfits·looks — 모두 Mock AI 포트)
7. `shared/storage`(Local) + `/images` 서빙
8. ai 레포: FastAPI 스켈레톤(엔드포인트 스텁) + Dockerfile → `ai.mode=http` 연동
9. 룩 이미지 **비동기 생성 훅**(`@Async` 또는 이벤트) + `GET /looks/{id}` 폴링 경로
10. 테스트(Testcontainers) 보강

> 순서 근거: 의존 방향이 `styling → closet·catalog` 이므로 **catalog → closet → styling** 순으로 쌓아야 Mock 없이 실제 참조가 이어진다.

---

## 13. 범위 밖 / 열린 항목

- **Gemini API 키·모델명·쿼터** (시은과) — 미해결 시 Mock 포트로 계속 진행.
- **상품 시드 적재** — MCM 공식몰은 Cloudflare 관리형 챌린지로 **서버 HTTP 크롤링 차단**(브라우저 접속은 정상). 반면 이미지 CDN `images.mcmworldwide.com/i/mcmworldwide/{SKU}_01` 은 서버에서 접근 가능하고 `?w=` 리사이즈도 먹는다.
  → **브라우저 세션으로 시드 150~200개 수확**(SKU·이름·가격·카테고리) → `resources/seed/mcm-products.json` → 적재 시 이미지 fetch + rembg로 `cutoutUrl` 생성.
  → **카테고리별 최소 수량 강제**: 가방만 있으면 `POST /outfits` 코디가 성립하지 않는다. 의류·신발 확보 필수.
  → 소스는 `ProductSource` 인터페이스로 추상화(`SeedJsonProductSource` / `PartnerFeedProductSource` / `CrawlerProductSource`). 주최측 공식 피드가 오면 교체.
- 보류 화면: AR(7)·예약및결제(8)·아카이브(11~13)·캘린더 화면(10)은 API만/후순위.
- ~~9 나의옷장 ↔ 14 디지털옷장 중복 검토~~ → **해결: 14 삭제, 9가 closet 탭.**
- ~~프로필(17) 후순위~~ → **MVP 승격**(하단탭 4개 중 하나). `GET /me` + 로그아웃.
