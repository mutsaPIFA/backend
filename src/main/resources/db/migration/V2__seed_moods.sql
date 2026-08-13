-- 무드 고정 시드 6개 (계약 api-v1.md §4-3)
-- ⚠️ 라벨은 Figma 무드 카드와 정렬 확정 대기 — 바뀌면 V3로 UPDATE (이 파일 수정 금지)

INSERT INTO moods (id, label, label_en, icon_key) VALUES
    (1, '저녁 약속', 'DINNER DATE',   'dinner'),
    (2, '출근',     'OFFICE',        'office'),
    (3, '출장',     'BUSINESS TRIP', 'trip'),
    (4, '데일리',   'CASUAL',        'daily'),
    (5, '주말 산책', 'WEEKEND WALK',  'walk'),
    (6, '파티',     'EVENT',         'party');

-- 명시적 id 삽입 뒤 시퀀스 보정
SELECT setval(pg_get_serial_sequence('moods', 'id'), 6);
