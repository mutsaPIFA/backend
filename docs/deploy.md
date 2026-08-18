# 배포 런북 — 가비아 g클라우드 (사용기간 2026-08-18 ~ 08-28)

> 구조 결정(2026-08-14): **한 VM에 nginx(프론트 정적 + 백엔드 프록시) + backend + ai + postgres를 Docker Compose로.**
> 같은 origin이라 쿠키(refresh)·CORS·mixed content 문제가 구조적으로 없다.
> 이미지 저장은 **VM 로컬 볼륨** (S3/CloudFront는 파킹 — 계약이 절대 URL이라 언제든 전환 가능).

```
[폰/브라우저] → 공인IP:80 (가비아 방화벽)
                └ nginx ── / (프론트 SPA, ../frontend/dist)
                        ├ /api, /images, /swagger-ui → backend:8080
                        └ (ai:8000·postgres는 외부 비노출 — 컨테이너 내부망 전용)
```

## 0. 사전 준비 (VM 받기 전에 끝낼 것)

- [ ] **신청 (마감 8/14 자정)**: 팀장 대표 1인 — 가비아 ID + 클라우드 ID **둘 다** 폼 제출 (하나만 내면 서버 미지급)
- [ ] `openssl rand -base64 48` 로 운영 JWT_SECRET 생성해 보관
- [ ] GEMINI_API_KEY 준비 (갠톡 — 레포·단체방 금지)

## 0-1. 서버 생성 시 주의 (8/18부터 가능 — 주최측 공지)

- 공인 IP 할당 = **"무조건 할당"** 선택 (필수)
- 사양은 안내된 것(High CPU 2vCore/4GB) **그대로** — 상향·부가 서비스 선택 시 팀장 결제수단으로 과금됨
- **8/28(금) 23:59 서버 일괄 삭제** — 그 전에 DB 덤프·업로드 볼륨 백업 (아래 §6, 데모 사진·계정 보존용)

## 1. 가비아 콘솔 — 방화벽

방화벽은 기본 **전체 차단**. 정책 등록:

| 포트 | 대상 | 용도 |
|---|---|---|
| 22 | 팀 IP만 (가능하면) | SSH |
| 80 | 전체 | 웹 (데모) |
| 443 | 전체 | (HTTPS 업그레이드 시) |

**8000(ai)·5432/5433(DB)는 열지 않는다** — 내부망 전용.

## 2. VM 초기 셋업 (SSH 접속 후)

서버 생성 시 root 임시 비밀번호가 메일로 옴 → 접속 후 비밀번호 변경.

```bash
# Docker + compose 설치 (Ubuntu)
curl -fsSL https://get.docker.com | sh
docker --version && docker compose version   # compose v2.24+ 확인 (env_file required 문법)

# 레포 클론 — backend·ai·frontend 나란히
mkdir -p /srv/mcmmuse && cd /srv/mcmmuse
git clone -b dev https://github.com/mutsaPIFA/backend.git
git clone -b dev https://github.com/mutsaPIFA/ai.git
git clone -b dev https://github.com/mutsaPIFA/frontend.git
```

## 3. 환경 변수

```bash
# ai 키 (팀 안내와 동일한 위치 — compose가 컨테이너에 자동 주입)
cat > ai/.env <<'EOF'
GEMINI_API_KEY=<갠톡으로 받은 키>
GEMINI_IMAGE_MODEL=gemini-2.5-flash-image
EOF

# backend compose 설정
cd backend && cp .env.example .env
# .env 에서 두 값 필수 교체:
#   JWT_SECRET=<0단계에서 생성한 값>
#   APP_STORAGE_PUBLIC_BASE_URL=http://<공인IP>/images
```

## 4. 프론트 빌드

```bash
cd /srv/mcmmuse/frontend && npm ci && npm run build   # 산출물 dist/ → nginx가 마운트
```

(빌드 도구가 VM에 없으면 로컬 빌드 후 `scp -r dist root@<공인IP>:/srv/mcmmuse/frontend/` 도 가능)

## 5. 기동·확인

```bash
cd /srv/mcmmuse/backend
docker compose up --build -d
docker compose ps                      # 4개 컨테이너 (postgres·ai·backend·nginx)
curl -s http://localhost/actuator/health   # {"status":"UP"}
```

외부 확인: `http://<공인IP>/` (프론트) · `http://<공인IP>/swagger-ui/index.html`

- 시드 146건은 부팅 시 자동 적재, 누끼 백필은 백그라운드 수 분 (실패분은 재기동 시 재시도)
- 컨테이너는 도커가 자동 재시작하도록: `docker update --restart unless-stopped $(docker ps -q)`

## 6. 운영 메모

- **로그**: `docker compose logs -f backend ai`
- **백업 (8/28 삭제 전 필수)**: DB `docker exec mcmmuse-postgres pg_dump -U mcmmuse mcmmuse > backup_$(date +%m%d).sql` + 이미지 `tar czf uploads_$(date +%m%d).tgz backend/uploads` — 업로드는 호스트 폴더 마운트라 폴더 복사가 곧 백업
- **재배포**: `git pull` 후 `docker compose up --build -d` (postgres 볼륨·`backend/uploads` 폴더는 유지됨)
- **Swagger 외부 노출**이 부담되면 `nginx/default.conf`의 swagger 블록 주석 처리 후 `docker compose restart nginx`
- HTTPS 업그레이드(선택): 도메인 연결 → certbot — nginx 설정만 추가하면 됨. cookie-secure 는 그때 `APP_JWT_COOKIE_SECURE=true`

## 파킹랏

- S3/CloudFront 전환 (`StorageService` 인터페이스 뒤 — store/load/resolveUrl/keyOf 구현만)
- HTTPS·도메인 · Swagger 접근 제한 · 모니터링/메트릭
