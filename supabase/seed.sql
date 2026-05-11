-- Clarix demo seed
--
-- 사전 준비:
--   1) Supabase Dashboard > Authentication > Users 에서 4명 만들기:
--      - dr.kim@clarix.demo  (의사)
--      - patient1@clarix.demo (환자A — 순응도 90%)
--      - patient2@clarix.demo (환자B — 60%)
--      - patient3@clarix.demo (환자C — 30%)
--   2) 각 user의 UUID를 받아 아래 :doctor_id, :p1, :p2, :p3 변수에 채워 실행.
--      psql 사용 시:  psql ... -v doctor_id=... -v p1=... -v p2=... -v p3=... -f seed.sql
--      Supabase SQL Editor 사용 시: \set 대신 \\:변수 형태 사용 불가 — 직접 치환해서 붙여넣기.
--
-- 모두 idempotent하지 않습니다. 테이블이 비어있는 상태에서 실행하세요.

-- 프로필
INSERT INTO public.users (id, role, name) VALUES
  (:'doctor_id', 'doctor',  '김의사'),
  (:'p1',        'patient', '김환자'),
  (:'p2',        'patient', '이환자'),
  (:'p3',        'patient', '박환자');

-- 권한 (모두 의사에게 active 공유)
INSERT INTO public.permissions (patient_id, doctor_id, is_active) VALUES
  (:'p1', :'doctor_id', TRUE),
  (:'p2', :'doctor_id', TRUE),
  (:'p3', :'doctor_id', TRUE);

-- 처방 — 모두 같은 약 1개, 아침/저녁 (하루 2회 = 7일 14회 기대)
INSERT INTO public.prescriptions (patient_id, medication_name, schedule, is_active) VALUES
  (:'p1', '콘서타 18mg', ARRAY['morning','evening'], TRUE),
  (:'p2', '아빌리파이 5mg', ARRAY['morning','evening'], TRUE),
  (:'p3', '리스페달 1mg', ARRAY['morning','evening'], TRUE);

-- 7일 medication_logs
-- patient1: 90% 순응 (14회 중 13회 taken, 1회 missed)
-- patient2: 60% (14회 중 8 taken, 6 missed)
-- patient3: 30% (14회 중 4 taken, 10 missed)

DO $$
DECLARE
  d_offset INT;
  taken_quota INT;
  patient_uid UUID;
  med_name TEXT;
  -- 14 슬롯의 실패 인덱스 (0-based, [day*2 + slot])
  fail_set INT[];
  i INT;
  slot_hour INT;
  ts TIMESTAMPTZ;
  status med_status;
  fail_idx INT;
BEGIN
  FOR patient_uid, med_name, taken_quota IN VALUES
    (:'p1'::UUID, '콘서타 18mg',     13),
    (:'p2'::UUID, '아빌리파이 5mg',  8),
    (:'p3'::UUID, '리스페달 1mg',    4)
  LOOP
    -- 결정적 분포: 같은 간격으로 taken 슬롯 선택
    fail_set := ARRAY[]::INT[];
    -- 14 - taken_quota 개의 missed 인덱스
    FOR i IN 0..(14 - taken_quota - 1) LOOP
      fail_set := array_append(fail_set, (i * 14 / GREATEST(14 - taken_quota, 1))::INT);
    END LOOP;

    FOR i IN 0..13 LOOP
      d_offset := 6 - (i / 2);  -- i=0,1 → 6일 전, i=12,13 → 0일 전 (오늘)
      slot_hour := CASE WHEN i % 2 = 0 THEN 8 ELSE 20 END;
      ts := date_trunc('day', NOW() - (d_offset || ' days')::INTERVAL) + (slot_hour || ' hours')::INTERVAL;
      status := CASE WHEN i = ANY(fail_set) THEN 'missed'::med_status ELSE 'taken'::med_status END;
      INSERT INTO public.medication_logs (patient_id, medication_name, status, taken_at)
      VALUES (patient_uid, med_name, status, ts);
    END LOOP;
  END LOOP;
END $$;

-- 7일 symptom_logs (patient별로 다른 패턴)
DO $$
DECLARE
  d_offset INT;
  the_date DATE;
BEGIN
  FOR d_offset IN 0..6 LOOP
    the_date := (CURRENT_DATE - d_offset);
    -- patient1: 안정적 (4-5)
    INSERT INTO public.symptom_logs (patient_id, mood_score, symptoms, note, log_date)
    VALUES (:'p1'::UUID, 4 + (d_offset % 2), '{"피로": true}'::jsonb, '컨디션 양호', the_date);
    -- patient2: 변동적 (2-4)
    INSERT INTO public.symptom_logs (patient_id, mood_score, symptoms, note, log_date)
    VALUES (:'p2'::UUID, 2 + (d_offset % 3), '{"두통": true, "어지러움": true}'::jsonb, '약 효과 들쭉날쭉', the_date);
    -- patient3: 저조 (1-3)
    INSERT INTO public.symptom_logs (patient_id, mood_score, symptoms, note, log_date)
    VALUES (:'p3'::UUID, 1 + (d_offset % 3), '{"불면": true, "식욕저하": true, "피로": true}'::jsonb, '최근 우울감 심함', the_date);
  END LOOP;
END $$;

-- SOAP 1건씩
INSERT INTO public.clinical_notes (doctor_id, patient_id, subjective, objective, assessment, plan) VALUES
  (:'doctor_id', :'p1', '컨디션 안정', '복약 순응도 90%, 평균 기분 4.5', '치료 반응 양호', '현 처방 유지, 4주 후 재방문'),
  (:'doctor_id', :'p2', '효과 변동성 호소', '복약 순응도 60%, 기분 변동 2-4', '용량 조정 검토', '아빌리파이 7.5mg로 증량, 2주 후 재평가'),
  (:'doctor_id', :'p3', '우울감 심화, 복약 거부 빈번', '순응도 30%, 평균 기분 2.0', '복약 순응도 위험 — 약물 변경 필요', '리스페달 중단, 다음 방문 시 sertraline 50mg 시작 검토');
