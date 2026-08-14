-- 제품 상세 보강 (계약 §2 — 화면 6/7-a 제품 상세)
-- CSV 원천에 이미 있던 상세 데이터 적재: 설명·사이즈·캐러셀 이미지들.

ALTER TABLE mcm_products ADD COLUMN description text;
-- 사이즈는 '|' 구분 목록일 수 있음 (신발은 최대 140자 실측) — text
ALTER TABLE mcm_products ADD COLUMN item_size text;
ALTER TABLE mcm_products ADD COLUMN image_urls text;
