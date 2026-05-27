# Clarix

**Unified Cognition. Clarity on Demand.**

정신건강·만성질환 환자의 자가 보고(PRO)와 의료진 임상 기록을 통합하여,
짧은 진료 시간에 정확한 약물 조정을 돕는 EMR형 반응형 웹서비스입니다.

## 기술 스택

| 영역 | 기술 |
|---|---|
| 빌드 | Gradle |
| 백엔드 | Java 21 · Spring Boot 3.5 · Spring MVC |
| 화면 | Thymeleaf · HTML5 · CSS3 · JavaScript |
| 데이터 | Spring Data JPA · Hibernate · **MySQL 8** |
| 인증 | Spring Security · BCrypt · 세션 로그인 |
| DTO | Lombok 기반 form/request DTO |
| 시각화 | Chart.js 4.x |

## 과제 요구사항 매핑

| 요구사항 | 구현 위치 |
|---|---|
| 반응형 웹 Frontend | `templates/`, `static/css/patient.css`, `static/css/doctor.css` |
| Spring Boot Backend | `PatientApiApplication.java`, `web/`, `service/` |
| DTO 기반 데이터 처리 | `dto/` + Controller `@ModelAttribute` 바인딩 |
| MySQL 기반 CRUD | `application.properties`, JPA `domain/`, `repo/`, `service/` |
| REST API / JSON | `DoctorApiController` timeline, drug-check, llm-soap |

## 디렉토리 구조

```text
maum_med/
├── AGENTS.md
├── prd.md
├── docs/
│   ├── decisions.md
│   ├── handoff.md
│   ├── issues.md
│   ├── memory.md
│   ├── prd-demo-stabilization.md
│   └── 실행방법.md
└── patient-api/
    ├── build.gradle
    ├── gradlew
    └── src/main/
        ├── java/com/clarix/
        │   ├── PatientApiApplication.java
        │   ├── config/      # SecurityConfig, DemoDataInitializer
        │   ├── domain/      # JPA Entity
        │   ├── dto/         # DTO
        │   ├── repo/        # JpaRepository CRUD
        │   ├── service/     # 비즈니스 로직
        │   └── web/         # MVC Controller, REST Controller
        └── resources/
            ├── application.properties  # MySQL 설정
            ├── static/
            └── templates/
```

## 에이전트 운영 방식

이 프로젝트는 장기 작업 관리를 위해 하네스 엔지니어링 문서를 사용합니다.

| 문서 | 용도 |
|---|---|
| `AGENTS.md` | Codex/Claude Code가 따라야 할 작업 규칙 |
| `docs/handoff.md` | 현재 진행 상태와 다음 세션 인수인계 |
| `docs/memory.md` | 반복 실패, Railway/MySQL/AI SOAP 주의사항 |
| `docs/decisions.md` | 유지해야 할 아키텍처/운영 결정 |
| `docs/prd-demo-stabilization.md` | 데모 안정화 PRD |
| `docs/issues.md` | vertical slice 작업 목록 |

## MySQL 준비

MySQL 콘솔에서 아래 SQL을 실행합니다.

```sql
CREATE DATABASE clarix CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'clarix'@'localhost' IDENTIFIED BY 'clarix1234';
GRANT ALL PRIVILEGES ON clarix.* TO 'clarix'@'localhost';
FLUSH PRIVILEGES;
```

## 실행

```bash
cd patient-api
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=clarix
export MYSQL_USER=clarix
export MYSQL_PASSWORD=clarix1234
./gradlew bootRun
```

부팅 완료 후 브라우저에서 접속합니다.

```text
http://localhost:8081
```

첫 실행 시 `DemoDataInitializer`가 병원, 사용자, 처방, 복약 기록, 감정 기록, PHQ-9, SOAP 데이터를 MySQL에 자동 생성합니다.
이미 사용자 데이터가 있으면 기존 데이터를 보존하기 위해 시드를 건너뜁니다.

## 데모 계정

| 역할 | 이메일 | 비밀번호 |
|---|---|---|
| 관리자 | `admin@clarix.demo` | `clarix1234` |
| 의사 | `dr.kim@clarix.demo` | `clarix1234` |
| 간호사 | `nurse@clarix.demo` | `clarix1234` |
| 검사기사 | `tech@clarix.demo` | `clarix1234` |
| 접수 | `desk@clarix.demo` | `clarix1234` |
| 환자 안정 | `patient1@clarix.demo` | `clarix1234` |
| 환자 변동 | `patient2@clarix.demo` | `clarix1234` |
| 환자 위험 | `patient3@clarix.demo` | `clarix1234` |
| 환자 테스트 | `patient4@clarix.demo` | `clarix1234` |

## 주요 화면

**환자**
- `/patient/` Today: 복약, 감정, 식사, 운동 기록
- `/patient/assessment`: PHQ-9 검사
- `/patient/onboarding`: 복용 약 등록/중단
- `/patient/calendar`: 감정 캘린더
- `/patient/sharing`: 의사 권한 관리

**의사**
- `/doctor/`: 환자 목록, 위험도 정렬, 7일 순응도
- `/doctor/patient/{id}`: 환자 상세, Chart.js timeline, SOAP, 처방
- `/doctor/admin`: 직원 초대/관리

**운영**
- `/reception/`: 예약 및 연락 메모
- `/staff/`: 간호사/검사기사 환자 차팅
- `/admin/`: 병원과 사용자 관리

## JSON API

| Method | Endpoint | 설명 |
|---|---|---|
| GET | `/doctor/patient/{id}/timeline` | 의사 차트용 timeline JSON |
| POST | `/doctor/drug-check` | 처방 전 약물 상호작용 체크 JSON |
| POST | `/doctor/patient/{id}/llm-soap` | 자유 텍스트 기반 SOAP 자동 작성 JSON |

## 문제 해결

| 증상 | 해결 |
|---|---|
| 8081 포트 사용 중 | `lsof -ti :8081 \| xargs kill -9` 후 재실행 |
| MySQL 접속 실패 | DB 생성, 계정 권한, `MYSQL_*` 환경변수 확인 |
| 테이블이 없음 | `spring.jpa.hibernate.ddl-auto=update` 설정 확인 후 재실행 |
| 데모 데이터가 없음 | `users` 테이블이 비어 있는지 확인. 비어 있을 때만 자동 시드 |
| 로그인 실패 | 데모 계정 이메일/비밀번호를 그대로 입력 |
