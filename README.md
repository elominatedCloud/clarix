# Clarix

**Unified Cognition. Clarity on Demand.**

정신건강·만성질환 환자의 자가 보고(PRO)와 의료진 임상 기록을 통합하여,
짧은 진료 시간에 정확한 약물 조정을 돕는 EMR.

## 스택

| 영역 | 기술 |
|---|---|
| 빌드 | **Gradle** (`build.gradle`) |
| 백엔드 | Spring Boot 3.5 · Java 21 · **Spring MVC + Thymeleaf** |
| 데이터 | **JPA + Spring Data JPA**, H2 in-memory (default) / PostgreSQL via Supabase |
| 인증 | **Spring Security** + BCrypt (폼 로그인 + 세션) |
| 프론트 | HTML5 + CSS3 (정적 자원), 차트만 Chart.js |
| 시각화 | Chart.js 4.x (의사 환자 상세 차트) |

수업 진도 매핑:
- 3주 HTML/CSS3 → `static/css/{tokens,patient,doctor}.css`
- 5주 Gradle 의존성 → `build.gradle`
- 6-7주 Controller + RESTful + Repository · Service 티어 → `web/`, `repo/`, `service/`
- 9주 JPA + ORM → `domain/` (9 엔티티)
- 11주 Thymeleaf + 정적 리소스 → `templates/`
- 12주 Spring Security 폼 로그인 → `config/SecurityConfig.java`

## 디렉토리 구조

```
maum_med/
├── prd.md
├── supabase/                # 참조용 SQL (Supabase profile에서 사용 가능)
└── patient-api/             # 단일 Spring Boot 애플리케이션
    ├── build.gradle
    ├── gradlew(.bat)
    └── src/main/
        ├── java/com/clarix/
        │   ├── PatientApiApplication.java
        │   ├── config/      # SecurityConfig, DemoDataInitializer
        │   ├── domain/      # JPA 엔티티 9개
        │   ├── repo/        # JpaRepository 인터페이스 8개
        │   ├── service/     # 비즈니스 로직 (Auth/Patient/Doctor/Assessment 등)
        │   └── web/         # @Controller (Home/Patient/Doctor)
        └── resources/
            ├── application.properties           # H2 default
            ├── application-supabase.properties  # 공유 데모용 PostgreSQL
            ├── data.sql                         # 병원 8개 시드 (자동)
            ├── static/css/                      # CSS 토큰 + 환자 + 의사
            └── templates/                       # Thymeleaf 페이지 13개
```

## 실행

### IntelliJ (권장)

1. `File → Open` → `patient-api/build.gradle` 선택 → `Open as Project`
2. 의존성 자동 import 후 `PatientApiApplication.java` 우클릭 → Run
3. 브라우저에서 http://localhost:8081

### 터미널

```bash
cd patient-api
./gradlew bootRun
```

부팅 완료 후 (~5초) 브라우저에서 http://localhost:8081

### Supabase profile (모든 PC가 같은 데이터)

`patient-api/.env`에 JDBC 정보 채우고:
```bash
export SUPABASE_JDBC_URL='jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres'
export SUPABASE_JDBC_USER='postgres.<project_ref>'
export SUPABASE_JDBC_PASSWORD='<password>'
./gradlew bootRun --args='--spring.profiles.active=supabase'
```

## 데모 계정 (H2 모드 자동 시드)

부팅 시 `DemoDataInitializer`가 자동 생성:

| 역할 | 이메일 | 비밀번호 |
|---|---|---|
| 의사 | `dr.kim@clarix.demo` | `clarix1234` |
| 환자 (안정) | `patient1@clarix.demo` | `clarix1234` |
| 환자 (변동) | `patient2@clarix.demo` | `clarix1234` |
| 환자 (위험) | `patient3@clarix.demo` | `clarix1234` |

각 환자에 7일치 복약 기록·감정 일기·PHQ-9·SOAP가 시드됨.

## 화면

**환자**
- `/` 랜딩 → `/auth/login` → `/patient/`
- `/patient/` Today (감정 카드 + 시간대별 약 봉투 + 모드별 헤더)
- `/patient/welcome` 병원 연결
- `/patient/assessment` PHQ-9 (9문항)
- `/patient/onboarding` 약 등록·관리
- `/patient/mood` → `/mood/journal` → `/mood/preview` → `/mood/save` (4-step 감정 일기)
- `/patient/sharing` 의사 권한 관리

**의사**
- `/doctor/` 환자 목록 (위험도 정렬, 7일 순응도, PHQ-9, 마지막 SOAP)
- `/doctor/patient/{id}` 환자 상세 (탭 차트 + 치료 타임라인 + SOAP 작성)

## DB 콘솔 (H2)

http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:clarix`
- User: `sa`, Password: 비움

## 트러블슈팅

| 증상 | 해결 |
|---|---|
| `Web server failed to start. Port 8081 was already in use.` | `lsof -ti :8081 \| xargs kill -9` 후 재시도 |
| 회원가입 후 로그인 실패 | 데모 계정으로 시도. 직접 가입 시 BCrypt가 정상 작동 |
| Supabase 모드에서 schema 불일치 | Supabase SQL Editor에서 `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` 후 재부팅 (JPA가 자동 생성) |
