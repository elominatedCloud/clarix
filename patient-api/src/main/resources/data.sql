-- H2 부팅 시 자동 실행. defer-datasource-initialization=true 라서 DDL 후에 실행됨.
-- 병원 8개 (5 연동 + 3 미연동)

INSERT INTO hospitals (id, name, address, specialty, is_partnered, distance_km, created_at) VALUES
  (RANDOM_UUID(), '마음정신건강의학과의원',         '서울 강남구 테헤란로 152',          '정신건강의학과',     TRUE,  1.2, CURRENT_TIMESTAMP),
  (RANDOM_UUID(), '서울대학교병원 정신건강의학과', '서울 종로구 대학로 101',            '정신건강의학과',     TRUE,  3.4, CURRENT_TIMESTAMP),
  (RANDOM_UUID(), '연세아이정신건강의학과',         '서울 서대문구 신촌로 134',          '정신건강의학과',     TRUE,  4.1, CURRENT_TIMESTAMP),
  (RANDOM_UUID(), '성북마음클리닉',                 '서울 성북구 동소문로 89',           '정신건강의학과',     TRUE,  5.8, CURRENT_TIMESTAMP),
  (RANDOM_UUID(), '판교웰니스의원',                 '경기 성남시 분당구 판교역로 235',   '내과·정신건강의학과', TRUE, 12.5, CURRENT_TIMESTAMP),
  (RANDOM_UUID(), '하늘마음의원',                   '서울 마포구 양화로 188',            '정신건강의학과',     FALSE, 2.7, CURRENT_TIMESTAMP),
  (RANDOM_UUID(), '새벽병원 정신건강센터',          '서울 영등포구 여의대로 24',         '정신건강의학과',     FALSE, 6.9, CURRENT_TIMESTAMP),
  (RANDOM_UUID(), '편안한 정신건강의원',            '서울 송파구 올림픽로 240',          '정신건강의학과',     FALSE, 8.3, CURRENT_TIMESTAMP);
