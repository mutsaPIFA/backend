-- 룩에 코디 컨셉명 저장 (계약 §4-4·4-5 concept — LLM 작명, 영어 2~3단어)
-- 후보에서 고른 concept을 저장 시 함께 남긴다. LLM 폴백(룰베이스)이었으면 null.

ALTER TABLE looks ADD COLUMN concept varchar(60);
