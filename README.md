# Clarix

**Unified Cognition. Clarity on Demand.**

정신건강·만성질환 환자의 자가 보고(PRO)와 의료진 임상 기록을 통합하여,
짧은 진료 시간에 정확한 약물 조정을 돕는 EMR.

## 기술 스택

| 영역 | 기술 |
|---|---|
| 빌드 | **Gradle** (`build.gradle`) |
| 백엔드 | Java 21 · Spring Boot 3.5 · Spring MVC |
| 화면 | Thymeleaf · HTML5 · CSS3 · JavaScript |
| 데이터 | Spring Data JPA · Hibernate · H2 in-memory |
| 인증 | **Spring Security** + BCrypt (폼 로그인 + 세션) |
| 코드 간소화 | Lombok |
| 시각화 | Chart.js 4.x |

### 스택 설명

- **Spring Boot**: 내장 Tomcat으로 서버를 실행하고, Controller/Service/Repository 구조를 관리.
- **Spring MVC**: `@Controller`, `@RestController`, `@PathVariable`, `@RequestParam`, `@ModelAttribute`로 URL과 폼 요청 처리.
- **Thymeleaf**: 서버에서 HTML 화면을 렌더링. 환자/의사/접수/관리자 화면을 `templates/`에서 관리.
- **Spring Data JPA + Hibernate**: Java 엔티티를 DB 테이블과 매핑하고 `JpaRepository`로 CRUD 처리.
- **H2 in-memory DB**: 별도 DB 설치 없이 실행 가능. 앱 재시작 시 데모 데이터가 다시 생성됨.
- **Spring Security + BCrypt**: 폼 로그인, 역할별 접근 제어, 비밀번호 해시 처리.
- **Lombok**: DTO와 엔티티의 getter/setter/생성자 반복 코드를 줄임.
- **Chart.js**: 의사 환자 상세 화면의 복약/감정/PHQ-9 차트 표시.

수업 진도 매핑:
- 3주 HTML/CSS3 → `static/css/{tokens,patient,doctor}.css`
- 4주 Spring Boot + Lombok → `build.gradle`, `dto/PrescriptionForm.java`, `domain/`
- 5주 Gradle 의존성 → `build.gradle`
- 6-7주 Controller + RESTful + Repository · Service 티어 → `web/`, `repo/`, `service/`
- 9주 JPA + ORM → `domain/` (9 엔티티)
- 11주 Thymeleaf + 정적 리소스 → `templates/`
- 12주 Spring Security 폼 로그인 → `config/SecurityConfig.java`

## 디렉토리 구조

```
maum_med/
├── prd.md
└── patient-api/             # 단일 Spring Boot 애플리케이션
    ├── build.gradle
    ├── gradlew(.bat)
    └── src/main/
        ├── java/com/clarix/
        │   ├── PatientApiApplication.java
        │   ├── config/      # SecurityConfig, DemoDataInitializer
        │   ├── domain/      # JPA 엔티티 9개
        │   ├── dto/         # Lombok DTO
        │   ├── repo/        # JpaRepository 인터페이스 8개
        │   ├── service/     # 비즈니스 로직 (Auth/Patient/Doctor/Assessment 등)
        │   └── web/         # @Controller, @RestController
        └── resources/
            ├── application.properties  # H2 기본 설정
            ├── data.sql                # 병원 8개 시드
            ├── static/                 # CSS, JS, 이미지
            └── templates/              # Thymeleaf 화면
```

## 실행

### IntelliJ (권장)

1. `File → Open` → `patient-api/build.gradle` 선택 → `Open as Project`
2. 의존성 자동 import 후 `PatientApiApplication.java` 우클릭 → Run
3. 브라우저에서 http://localhost:8081

### 터미널

별도 `.env`나 외부 DB 설정 없이 H2 in-memory DB로 실행됩니다.

```bash
cd patient-api
./gradlew bootRun
```

부팅 완료 후 (~5초) 브라우저에서 http://localhost:8081

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
| 데이터가 재시작 후 사라짐 | H2 in-memory DB라 정상 동작. 실행할 때마다 데모 데이터가 다시 생성됨 |
| H2 콘솔 접속 실패 | JDBC URL이 `jdbc:h2:mem:clarix`, User가 `sa`, Password가 비어 있는지 확인 |
| Lombok getter/setter를 IDE가 인식하지 못함 | IntelliJ에서 Annotation Processing을 활성화하거나 Gradle로 빌드 |
