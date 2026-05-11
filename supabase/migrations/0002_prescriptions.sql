-- Migration #2: prescriptions table
-- 환자가 등록한 활성 처방약. PAT-01 일일 복약 토글이 이 목록을 순회.

CREATE TABLE public.prescriptions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    patient_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    medication_name VARCHAR(200) NOT NULL,
    schedule TEXT[] NOT NULL,   -- ['morning','noon','evening'] 부분집합
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_prescriptions_patient_active
    ON public.prescriptions(patient_id) WHERE is_active = TRUE;

ALTER TABLE public.prescriptions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Patients manage own prescriptions"
    ON public.prescriptions FOR ALL
    USING (auth.uid() = patient_id)
    WITH CHECK (auth.uid() = patient_id);

CREATE POLICY "Doctors view permitted patients prescriptions"
    ON public.prescriptions FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.permissions p
            WHERE p.doctor_id = auth.uid()
              AND p.patient_id = prescriptions.patient_id
              AND p.is_active = TRUE
        )
    );
