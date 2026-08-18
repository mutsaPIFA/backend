-- 프로필·수정·찜 (계약 §1-5 확장 · §3-6 · §4-9 · §5)

-- §5-2 프로필 이미지
ALTER TABLE users ADD COLUMN avatar_url TEXT;

-- §3-6 옷장 아이템 사용자 지정 명칭 (null이면 프론트가 태그 조합으로 표시)
ALTER TABLE closet_items ADD COLUMN custom_name VARCHAR(30);

-- §1-5 styleDna — 최근 스타일 DNA 스냅샷 (1:1, §4-1 성공 시 갱신)
CREATE TABLE user_style_dna (
    user_id         BIGINT PRIMARY KEY REFERENCES users (id),
    summary         TEXT        NOT NULL,
    dominant_colors TEXT,                    -- '|' 구분 목록 (StringListConverter)
    dominant_moods  TEXT,
    keywords        TEXT,
    updated_at      TIMESTAMPTZ NOT NULL
);

-- §5-3~5-5 찜
CREATE TABLE wishlist_items (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users (id),
    mcm_product_id BIGINT      NOT NULL REFERENCES mcm_products (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, mcm_product_id)
);

CREATE INDEX idx_wishlist_user ON wishlist_items (user_id, created_at DESC);
