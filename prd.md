# 📄 Product Requirements Document (PRD): Project Clarix

**문서 버전:** v1.0
**프로젝트명:** Clarix (클라릭스)
**슬로건:** Unified Cognition. Clarity on Demand. (통합된 인지. 온디맨드 명확성.)
**작성일:** 2026-04-29

---

## 1. 프로젝트 개요 (Product Overview)
**Clarix**는 정신건강 및 만성질환 환자의 자가 보고 데이터(PRO)와 의료진의 임상 처방 기록을 통합하여, 의사의 인지 부하를 줄이고 정확한 약물 조정(Titration)을 돕는 하이브리드 EMR 대시보드 및 환자 트래킹 앱입니다. 환자의 모호한 진술을 데이터 기반의 명확한 임상 신호로 변환하는 것을 목표로 합니다.

## 2. 타겟 유저 (Target Audience)
1. **환자 (Patient):** 정신건강의약품 등 지속적인 약물 용량 조절과 부작용 모니터링이 필요한 환자.
2. **의료진 (Doctor):** 짧은 진료 시간 내에 환자의 복약 순응도와 부작용 패턴을 직관적으로 파악하고 처방을 결정해야 하는 전문의.

---

## 3. 시스템 아키텍처 (Hybrid Architecture)
본 프로젝트는 제한된 리소스와 배포 안정성을 고려하여 **'하이브리드 로컬-클라우드 모델'**을 채택합니다.

* **FrontEnd (로컬):** HTML5, CSS3, Vanilla JS, Chart.js (브라우저 실행)
* **BackEnd 1 - Patient API (로컬):** Spring Boot (데이터 무결성이 중요한 환자 데이터 수집)
* **BackEnd 2 - Doctor API (로컬):** Django (통계 및 차트 데이터 가공 특화 분석 서버)
* **DataBase (클라우드):** Supabase (PostgreSQL) - 공용 원격 데이터베이스
* **Network / Demo:** Ngrok (외부 시연용 로컬 서버 터널링)

---

## 4. 핵심 요구사항 (Core Requirements)

### 4.1 환자용 모바일 웹 (Patient App)
| 기능 식별자 | 기능명 | 상세 설명 | 담당 서버 |
| :--- | :--- | :--- | :--- |
| `PAT-01` | **간편 복약 체크** | 오늘 처방받은 약의 아침/점심/저녁 복용 여부를 토글로 기록 | Spring Boot |
| `PAT-02` | **감정 및 증상 트래킹** | 1~5점 척도로 오늘의 기분 기록 및 주요 부작용(어지러움 등) 체크 | Spring Boot |
| `PAT-03` | **데이터 연동 (Clinic Connect)** | 의료진에게 내 기록을 공유하도록 승인하는 권한 제어 버튼 | Spring Boot |

### 4.2 의사용 대시보드 (Doctor Dashboard)
| 기능 식별자 | 기능명 | 상세 설명 | 담당 서버 |
| :--- | :--- | :--- | :--- |
| `DOC-01` | **직원 권한 관리 (RBAC)** | 의사(전체), 간호사(모니터링), 원무(조회) 등 직군별 접근 제어 | Django |
| `DOC-02` | **위험 환자 알림 보드** | 복약 순응도 50% 미만 등 위험 임계값 초과 환자 자동 상단 노출 | Django |
| `DOC-03` | **복합 패턴 차트 (Chart.js)** | 막대 차트(복약량)와 선 차트(부작용)를 겹쳐 표시하여 상관관계 분석 | Django |
| `DOC-04` | **치료 이력 타임라인** | 과거 방문 및 처치 기록, 주요 감정 에피소드를 시간순 나열 | Django |
| `DOC-05` | **SOAP 임상 차팅** | 진료 내용을 주관적/객관적 데이터로 나누어 기록하고 처방 연계 | Django |

---

## 5. 데이터베이스 스키마 초안 (Supabase Tables)
1. **`users`**: 환자 및 의료진 계정 정보 (Role 필드로 권한 분리)
2. **`medication_logs`**: 환자의 일일 복약 여부 기록 (Spring Boot Write)
3. **`symptom_logs`**: 환자의 기분 점수 및 부작용 텍스트 기록 (Spring Boot Write)
4. **`clinical_notes`**: 의사의 SOAP 차팅 기록 (Django Write/Read)
5. **`permissions`**: 환자-의사 간 데이터 공유 권한 매핑 테이블

---

## 6. 성공 지표 (Success Metrics - Class Project)
* 환자 데이터 입력부터 의사 대시보드 차트 렌더링까지의 지연 시간 < 1초
* Chart.js를 활용한 시각화 시 데이터 레이어(순응도/부작용) 교차 분석의 직관성
* Spring Boot와 Django 서버가 하나의 DB를 충돌 없이 공유하고 트랜잭션을 처리하는 무결성 증명
