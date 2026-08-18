-- 상품에 무드 라벨과 스타일 한 줄 요약 추가 (추천·코디 프롬프트 입력용)
-- mood: scripts/tag_product_mood.py 가 채우는 LLM 태깅 결과. 옷장 아이템의 mood 와 같은 vocabulary.
--       태깅 전 시드로 적재될 수 있어 nullable.
-- style_note: CSV shortDesc(평균 43자). 후보가 많아 description(평균 144자)은 프롬프트에 못 넣는다.

ALTER TABLE mcm_products ADD COLUMN mood varchar(20);
ALTER TABLE mcm_products ADD COLUMN style_note text;
