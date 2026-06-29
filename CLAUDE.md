# CLAUDE.md

Guidance for AI assistants (Claude Code, Codex) working in this repository.

> This file complements `AGENTS.md` (operating policy) and the `docs/` harness
> files. When this file and `AGENTS.md` disagree on process, follow `AGENTS.md`.
> Always read `docs/memory.md` before non-trivial work to avoid repeating known
> failures.

## What This Is

**Clarix** — *"Unified Cognition. Clarity on Demand."* — is a Spring Boot +
Thymeleaf EMR-style demo web app. Patients self-report (PRO: medication,
emotion, meals, exercise, PHQ-9), and clinicians review that data to make
faster, more accurate medication decisions in short consultations.

This is a **long-running demo product**, not a one-off task. Work as harnessed
engineering: preserve state, reduce ambiguity, slice changes vertically, and
verify before handing back.

The whole application lives in **`patient-api/`**. The repo root holds research
and harness docs (`prd.md`, `MARKET_RESEARCH.md`, `DISCOVERY_QUESTIONS.md`,
`UX_RESEARCH_FINDINGS.md`, `docs/`).

## Tech Stack

| Area | Technology |
|---|---|
| Build | Gradle (wrapper in `patient-api/`) |
| Language/Runtime | Java 21 (toolchain pinned in `build.gradle`) |
| Backend | Spring Boot 3.5 · Spring MVC |
| View | Thymeleaf · HTML5 · CSS3 · vanilla JS |
| Data | Spring Data JPA · Hibernate · **MySQL 8** |
| Auth | Spring Security · BCrypt · session login |
| Validation | Jakarta Bean Validation on form DTOs |
| Charts | Chart.js 4.x (CDN, in templates) |
| AI | Gemini REST (`gemini-2.0-flash`) for SOAP autofill |
| Deploy | Railway (Railpack builder) |

## Repository Layout

```text
clarix/
├── AGENTS.md                  # Agent operating policy (read first for process)
├── CLAUDE.md                  # This file
├── README.md                  # Korean run/usage guide, demo accounts
├── prd.md, *_RESEARCH.md      # Product/market/UX research docs
├── railway.json               # Railway deploy config
├── docs/
│   ├── backend-architecture.md   # Deep architecture reference (KR)
│   ├── decisions.md              # Durable architecture/ops decisions
│   ├── memory.md                 # Recurring failures + environment facts
│   ├── handoff.md                # Latest state + next slices
│   ├── issues.md                 # Vertical-slice work list
│   ├── prd-demo-stabilization.md
│   └── 실행방법.md / .pdf          # Run guide (KR)
└── patient-api/               # ← THE APP
    ├── build.gradle, gradlew
    └── src/
        ├── main/java/com/clarix/
        │   ├── PatientApiApplication.java
        │   ├── config/     # SecurityConfig, DemoDataInitializer
        │   ├── domain/     # JPA entities + enums
        │   ├── dto/        # *Form DTOs (validated)
        │   ├── repo/       # Spring Data JPA repositories
        │   ├── service/    # Business rules, authZ, transactions
        │   └── web/        # MVC Controllers + REST Controller
        ├── main/resources/
        │   ├── application.properties
        │   ├── static/{css,js,img}/
        │   └── templates/  # Thymeleaf views (per-role folders)
        └── test/java/com/clarix/
```

## Architecture & Layering

Standard layered architecture — **controllers never touch the DB directly**:

```
Browser → web (Controller/RestController) → service → repo → MySQL
                    ↑ dto (form binding + @Valid validation)
```

- `domain/` — JPA entities (`User`, `Hospital`, `Permission`, `Prescription`,
  `MedicationLog`, `SymptomLog`, `Assessment`, `ClinicalNote`, `CareRequest`,
  `StaffInvite`, ...) and enums (`Role`, `Emotion`, `MedStatus`, `Severity`,
  `NoteKind`, `MealKind`, `CareRequestStatus`).
- `repo/` — `JpaRepository` interfaces; query methods only.
- `dto/` — `*Form` classes bound via `@ModelAttribute`, carry Bean Validation
  annotations. Enum fields bind directly to enum types where possible.
- `service/` — business rules, transactions, authorization, multi-repo
  composition. Keep DB access here.
- `web/` — routing, current-user resolution, building the `Model`, redirects.
- `config/` — `SecurityConfig`, `DemoDataInitializer`.

See `docs/backend-architecture.md` for full diagrams and per-flow sequences.

## Authentication & Authorization

Spring Security **form login** (session-based, BCrypt). Two enforcement layers:

1. **URL level** — `config/SecurityConfig.java` gates by role prefix:
   - `/patient/**` → `PATIENT`
   - `/doctor/**` → `DOCTOR`
   - `/reception/**` → `RECEPTIONIST`
   - `/staff/**` → `NURSE` or `TECHNICIAN`
   - `/admin/**` → `ADMIN`
   - `/`, `/health`, `/auth/**`, `/css|js|img/**` are public.
2. **Service/controller level** — defense in depth. Controllers call
   `CurrentUser.requireRole(session, Role.X)`; sensitive data access re-checks
   ownership (e.g. `DoctorService.requireSharedPatient()` blocks a doctor from
   viewing a patient they have no active `Permission` for, even if the URL id is
   tampered).

Notes:
- CSRF uses a cookie-based token repository; `/reception/**` is CSRF-exempt (POST
  re-checks role in the controller) to survive Railway proxy/session edge cases.
- Login success routes by role to the role home (see `roleBasedSuccessHandler`).
- `CurrentUser` resolves the Security principal (email) to the `User` entity.

## Conventions

- **Package root** is `com.clarix`. Add new code under the matching layer
  package; keep controllers thin and push logic to services.
- **DTOs**: name `XxxForm`, validate with Jakarta annotations, pair `@Valid`
  with `BindingResult` in the controller, and redirect with a concise flash
  error via `web/ValidationFeedback.java`.
- **Time** is `Asia/Seoul` throughout (e.g. `CLINIC_ZONE` in `PatientController`).
- **JSON-in-MySQL**: `service/JsonStore.java` serializes `Map` ↔ JSON for TEXT
  columns (used for flexible blobs like assessment answers).
- **Comments** in the codebase are predominantly Korean and explain *why*; match
  the surrounding style and language when editing a file.
- **Demo data**: `DemoDataInitializer` seeds accounts/data **only when the users
  table is empty** — it never overwrites existing data.
- **Schema**: `spring.jpa.hibernate.ddl-auto=update` (no migrations). Intentional
  for the demo; production would move to Flyway/Liquibase (see `decisions.md`).
- **AI SOAP must fail explicitly** — surface configuration/provider/network
  errors distinctly, never hide everything behind a generic 503.

## Key Screens & APIs

**Patient** (`/patient/`): Today (medication/emotion/meal/exercise), `/assessment`
(PHQ-9), `/onboarding` (medications), `/calendar` (mood), `/sharing` (doctor
permissions).
**Doctor** (`/doctor/`): patient list with risk sorting + 7-day adherence,
`/doctor/patient/{id}` detail with Chart.js timeline + SOAP + prescription,
`/doctor/admin` staff management.
**Ops**: `/reception/` (appointments, contact memos), `/staff/` (nurse/technician
charting), `/admin/` (hospitals/users).

**JSON API** (`web/DoctorApiController.java`, `@RestController`):

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/doctor/patient/{id}/timeline` | Chart timeline JSON |
| GET | `/doctor/patient/{id}/mood-state` | Lightweight mood-change polling |
| POST | `/doctor/drug-check` | Drug-interaction check before prescribing |
| POST | `/doctor/patient/{id}/llm-soap` | Gemini-generated SOAP draft |
| GET | `/health` | Liveness/status probe |

## Build, Run, Test

All commands run from **`patient-api/`**.

```bash
# Build (run after Java/template/CSS changes)
./gradlew build

# Run tests only (no DB required — see note below)
./gradlew test

# Run locally
export MYSQL_HOST=localhost MYSQL_PORT=3306 \
       MYSQL_DATABASE=clarix MYSQL_USER=clarix MYSQL_PASSWORD=clarix1234
./gradlew bootRun
# → http://localhost:8081
```

MySQL must exist first:

```sql
CREATE DATABASE clarix CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'clarix'@'localhost' IDENTIFIED BY 'clarix1234';
GRANT ALL PRIVILEGES ON clarix.* TO 'clarix'@'localhost';
FLUSH PRIVILEGES;
```

A common local Docker MySQL setup uses `127.0.0.1:3307`, db/user `clarix`,
password `clarixpass` (see `docs/memory.md`).

Config is fully env-driven (`application.properties`): `PORT` (default 8081),
`MYSQL[_]HOST/PORT/DATABASE/USER/PASSWORD` (Railway `MYSQLHOST`-style names also
accepted), and `GEMINI_API_KEY`/`GOOGLE_API_KEY` for SOAP (empty ⇒ endpoint
returns an explicit error).

**Tests:** `PatientApiApplicationTests` only asserts the app class exists, and
`FormValidationTests` uses a standalone Bean Validator — so `./gradlew test`
does **not** require a running MySQL.

## Verification Expectations

- Java/template/CSS change → run `./gradlew build` from `patient-api/`.
- UI change → run locally and capture screenshots when feasible. Example:
  `npx playwright screenshot --viewport-size=390,844 http://127.0.0.1:8081/ out.png`.
- Deployment issue → read Railway logs for the first real root cause, not
  repeated wrapper exceptions.
- Before claiming a full build passed, ensure enough free disk (past failures hit
  `No space left on device` during `bootJar`).

## Deployment (Railway)

- Root directory **must** be `patient-api`.
- Start command runs the jar from `build/libs` (see `railway.json`).
- `SPRING_DATASOURCE_URL` must be **one line** with all query params inline; do
  not split JDBC params into separate env vars, and do not wrap values in quotes.
- Never use env var names containing whitespace.
- **Never commit secrets.** If an API key appears in chat/screenshots/git
  history, rotate it.

## Demo Accounts

All passwords are `clarix1234`. Emails: `admin@`, `dr.kim@`, `nurse@`, `tech@`,
`desk@`, and `patient1@`–`patient4@`, all `@clarix.demo`. (Full table in
`README.md` / `docs/handoff.md`.)

## Working Agreements

- Read `docs/handoff.md`, `docs/memory.md`, `docs/decisions.md` (and relevant
  PRD/issue docs) before substantial UI/backend/deploy/AI work.
- Work in **vertical slices** (UI + backend + data + error handling +
  verification); avoid broad unrelated refactors.
- The worktree may be dirty — never revert changes you did not make.
- Record new recurring failures in `docs/memory.md`; record durable decisions in
  `docs/decisions.md`.
- Git: develop on the assigned feature branch, commit with clear messages, push
  with `git push -u origin <branch>`, and open a draft PR after pushing.
