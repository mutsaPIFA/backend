-- 룩에 사용자 소감(note) 저장 (계약 §4-5 — 화면 11 "이 코디 어땠어요?")
-- AI 추천 이유(reason)와 별개인 사용자 자유 텍스트.

ALTER TABLE looks ADD COLUMN note text;
