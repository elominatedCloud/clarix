-- ==========================================
-- Clarix DB Schema
-- Supabase (PostgreSQL) — apply via SQL Editor
-- ==========================================

-- 1. ENUM 타입 정의 (역할 및 상태)
CREATE TYPE user_role AS ENUM ('patient', 'doctor', 'admin');
CREATE TYPE med_status AS ENUM ('taken', 'missed', 'delayed');

-- ==========================================
-- [테이블 생성]
-- ==========================================

-- 1. Users Table (Supabase Auth와 1:1 매핑)
CREATE TABLE public.users (
    id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    role user_role NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Permissions Table (환자가 의사에게 데이터 열람 권한 부여)
CREATE TABLE public.permissions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    patient_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    doctor_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT TRUE,
    granted_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(patient_id, doctor_id)
);

-- 3. Medication Logs (환자 복약 기록 - Spring Boot 수집)
CREATE TABLE public.medication_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    patient_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    medication_name VARCHAR(200) NOT NULL,
    status med_status NOT NULL,
    taken_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Symptom Logs (환자 기분/부작용 기록 - Spring Boot 수집)
CREATE TABLE public.symptom_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    patient_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    mood_score INT CHECK (mood_score BETWEEN 1 AND 5),
    symptoms JSONB,
    note TEXT,
    log_date DATE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Clinical Notes (의사 SOAP 차트 - Django 수집/분석)
CREATE TABLE public.clinical_notes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    doctor_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    patient_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    subjective TEXT,
    objective TEXT,
    assessment TEXT,
    plan TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 조회 성능을 위한 인덱스
CREATE INDEX idx_medication_logs_patient_taken ON public.medication_logs(patient_id, taken_at DESC);
CREATE INDEX idx_symptom_logs_patient_date ON public.symptom_logs(patient_id, log_date DESC);
CREATE INDEX idx_clinical_notes_patient ON public.clinical_notes(patient_id, created_at DESC);
CREATE INDEX idx_permissions_doctor_active ON public.permissions(doctor_id) WHERE is_active = TRUE;

-- ==========================================
-- [RLS (Row Level Security) 정책]
-- ==========================================
ALTER TABLE public.users           ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.permissions     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medication_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.symptom_logs    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.clinical_notes  ENABLE ROW LEVEL SECURITY;

-- ----- users -----
CREATE POLICY "Users can read their own profile"
    ON public.users FOR SELECT
    USING (auth.uid() = id);

CREATE POLICY "Users can insert their own profile on signup"
    ON public.users FOR INSERT
    WITH CHECK (auth.uid() = id);

CREATE POLICY "Doctors can read profiles of permitted patients"
    ON public.users FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.permissions p
            WHERE p.doctor_id = auth.uid()
              AND p.patient_id = users.id
              AND p.is_active = TRUE
        )
    );

-- ----- permissions -----
CREATE POLICY "Patients manage their own permission grants"
    ON public.permissions FOR ALL
    USING (auth.uid() = patient_id)
    WITH CHECK (auth.uid() = patient_id);

CREATE POLICY "Doctors can read permissions granted to them"
    ON public.permissions FOR SELECT
    USING (auth.uid() = doctor_id);

-- ----- medication_logs -----
CREATE POLICY "Patients can manage their own medication logs"
    ON public.medication_logs FOR ALL
    USING (auth.uid() = patient_id)
    WITH CHECK (auth.uid() = patient_id);

CREATE POLICY "Doctors can view permitted patients' medication logs"
    ON public.medication_logs FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.permissions p
            WHERE p.doctor_id = auth.uid()
              AND p.patient_id = medication_logs.patient_id
              AND p.is_active = TRUE
        )
    );

-- ----- symptom_logs -----
CREATE POLICY "Patients can manage their own symptom logs"
    ON public.symptom_logs FOR ALL
    USING (auth.uid() = patient_id)
    WITH CHECK (auth.uid() = patient_id);

CREATE POLICY "Doctors can view permitted patients' symptom logs"
    ON public.symptom_logs FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.permissions p
            WHERE p.doctor_id = auth.uid()
              AND p.patient_id = symptom_logs.patient_id
              AND p.is_active = TRUE
        )
    );

-- ----- clinical_notes -----
CREATE POLICY "Doctors manage their own clinical notes"
    ON public.clinical_notes FOR ALL
    USING (auth.uid() = doctor_id)
    WITH CHECK (auth.uid() = doctor_id);

CREATE POLICY "Patients can read clinical notes about themselves"
    ON public.clinical_notes FOR SELECT
    USING (auth.uid() = patient_id);
