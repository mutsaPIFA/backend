-- MCM MUSE 초기 스키마
-- 근거: docs 레포 api-contract-v1.md §3 데이터 모델 + 결정로그 D1~D14
-- 컨벤션: backend/docs/conventions/DB-컨벤션.md

-- ---------------------------------------------------------------- auth

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(100) NOT NULL,              -- BCrypt 해시
    nickname    VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email)        -- 애플리케이션에서 소문자 정규화 후 저장
);

-- refresh token 회전: 갱신마다 기존 row 삭제 후 새 row 삽입.
-- 로그아웃 = 해당 유저 row 삭제 → 서버에서 실제로 무효화된다.
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,              -- SHA-256(token). 원문은 저장하지 않는다
    expires_at  TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- ---------------------------------------------------------------- catalog

CREATE TABLE mcm_products (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(40)  NOT NULL,              -- MCM 공식몰 SKU. 시드 upsert 키
    name        VARCHAR(255) NOT NULL,
    category    VARCHAR(20)  NOT NULL,              -- controlled vocabulary (앱 검증)
    color       VARCHAR(20)  NOT NULL,
    material    VARCHAR(20)  NOT NULL,
    price       INTEGER      NOT NULL,              -- KRW 원 단위
    image_url   TEXT         NOT NULL,
    cutout_url  TEXT,                               -- 적재 시 rembg 생성, 실패 시 NULL
    product_url TEXT         NOT NULL,              -- 공식몰 PDP. 화면 6 "구매하기"
    active      BOOLEAN      NOT NULL DEFAULT TRUE, -- 피드에서 사라진 상품은 false (row 삭제 금지 — 옷장·룩이 FK 참조)
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_mcm_products_sku UNIQUE (sku)
);

-- 카탈로그 조회는 항상 active 필터
CREATE INDEX idx_mcm_products_active_category ON mcm_products (active, category);

-- ---------------------------------------------------------------- closet

CREATE TABLE closet_items (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category       VARCHAR(20) NOT NULL,            -- controlled vocabulary (앱 검증)
    color          VARCHAR(20) NOT NULL,
    material       VARCHAR(20) NOT NULL,
    mood           VARCHAR(20) NOT NULL,            -- ItemMood (큐레이터 Mood와 별개)
    image_url      TEXT        NOT NULL,
    cutout_url     TEXT,
    source         VARCHAR(10) NOT NULL,            -- OWN | MCM
    mcm_product_id BIGINT      REFERENCES mcm_products (id),  -- source=MCM 카탈로그 담기일 때만
    deleted_at     TIMESTAMPTZ,                     -- 소프트 삭제. 활성 조회는 deleted_at IS NULL
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 옷장 목록(GET /closet-items, createdAt DESC) 전용 부분 인덱스
CREATE INDEX idx_closet_items_user_active
    ON closet_items (user_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------- styling

-- 고정 시드 6개 (V2에서 삽입). 계약상 moodId 1~6.
CREATE TABLE moods (
    id       BIGSERIAL PRIMARY KEY,
    label    VARCHAR(30) NOT NULL,                  -- "저녁 약속"
    label_en VARCHAR(30) NOT NULL,                  -- "DINNER DATE"
    icon_key VARCHAR(20) NOT NULL                   -- "dinner"
);

CREATE TABLE looks (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    worn_date           DATE        NOT NULL,       -- 미전송 시 서버가 오늘 날짜 (D9)
    mood_id             BIGINT      NOT NULL REFERENCES moods (id),
    mcm_product_id      BIGINT      NOT NULL REFERENCES mcm_products (id),  -- 코디당 MCM 정확히 1개 (D5)
    reason              TEXT,
    generated_image_url TEXT,                       -- 비동기 생성 (D2). 완료 전/실패 시 NULL
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 코디 기록 (GET /looks?month=)
CREATE INDEX idx_looks_user_worn_date ON looks (user_id, worn_date);

-- Look ↔ 옷장 아이템. FK로 무결성 보장 — 아이템은 소프트 삭제라 참조가 깨지지 않는다.
CREATE TABLE look_closet_items (
    look_id        BIGINT NOT NULL REFERENCES looks (id) ON DELETE CASCADE,
    closet_item_id BIGINT NOT NULL REFERENCES closet_items (id),
    PRIMARY KEY (look_id, closet_item_id)
);

-- "이 옷이 들어간 룩" 역질의
CREATE INDEX idx_look_closet_items_item ON look_closet_items (closet_item_id);
