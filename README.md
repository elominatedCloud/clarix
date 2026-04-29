# Clarix

**Unified Cognition. Clarity on Demand.**

정신건강·만성질환 환자의 자가 보고(PRO)와 의료진 임상 기록을 통합하여, 짧은 진료 시간에 정확한 약물 조정을 돕는 하이브리드 EMR.

자세한 요구사항은 [`prd.md`](./prd.md) 참고.

## 모노레포 구조

```
maum_med/
├── prd.md                  요구사항 문서
├── supabase/
│   └── schema.sql          DB 스키마 + RLS 정책 (Supabase SQL Editor에 붙여넣어 실행)
├── frontend/               정적 HTML/CSS/JS, Chart.js, Supabase JS SDK
├── patient-api/            Spring Boot 4.0 (Java 21) — 환자 데이터 수집
└── doctor-api/             Django 5.2 (Python 3.14) — 분석/시각화
```

## 사전 준비

1. **Supabase 무료 티어 프로젝트 생성** → SQL Editor에 `supabase/schema.sql` 전체 붙여넣기 → Run.
2. Settings → API 에서 다음 값 확인:
   - Project URL
   - `anon` public key (프론트용)
   - `service_role` key (백엔드 전용 — 절대 프론트에 노출 금지)
   - JWT Secret
3. 각 서비스 폴더의 `.env.example`을 `.env`로 복사하고 위 값을 채움.
4. `frontend/config.js`의 `supabaseUrl` / `supabaseAnonKey`를 채움.

## 로컬 실행 (터미널 3개)

### Terminal 1 — patient-api (Spring Boot, port 8081)
```bash
cd patient-api
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run
```

### Terminal 2 — doctor-api (Django, port 8000)
```bash
cd doctor-api
source .venv/bin/activate
python manage.py migrate          # 최초 1회
python manage.py runserver
```

### Terminal 3 — frontend (정적 서버, port 5173)
```bash
cd frontend
python3 -m http.server 5173
```

브라우저에서 http://localhost:5173 접속.

## 외부 시연 (Ngrok)
```bash
ngrok http 5173        # 또는 8081 / 8000을 각각 터널링
```

## 개발 진행 상황
- [x] 모노레포 스켈레톤 (스키마 + 3개 서비스 부트스트랩)
- [ ] **수직 슬라이스 1: PAT-01 복약 체크** (프론트 → Spring Boot → Supabase E2E)
- [ ] PAT-02, PAT-03
- [ ] DOC-01 ~ DOC-05
