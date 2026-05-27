# Clarix Agent Operating Guide

This repository is a long-running demo product, not a one-off coding task. Treat work as harnessed engineering: preserve state, reduce ambiguity, slice changes vertically, and verify the result before handing it back.

## Project Shape

- App: Clarix, a Spring Boot 3.5 + Thymeleaf mental-health/clinical workflow demo.
- Main code: `patient-api/`
- Backend: Java 21, Spring MVC, Spring Security, Spring Data JPA, MySQL 8.
- Frontend: Thymeleaf templates, `static/css/patient.css`, `static/css/doctor.css`, small JavaScript where needed.
- Demo data: `DemoDataInitializer` seeds data only when users are empty.

## Required Reading Before Major Work

Read these files before substantial UI, backend, deployment, or AI changes:

- `docs/handoff.md`
- `docs/memory.md`
- `docs/decisions.md`
- Relevant PRD or issue docs under `docs/`

For small targeted fixes, read the directly affected files and `docs/memory.md`.

## Operating Loop

1. Clarify ambiguity before implementation.
   - Ask short "deep interview" questions when the target role, target screen, deployment scope, or acceptance criteria are unclear.
   - Do not ask when the repository context already gives a safe answer.
2. Work in vertical slices.
   - A slice should include UI/backend/data/error handling/verification where relevant.
   - Avoid broad unrelated refactors.
3. Preserve user work.
   - The worktree may be dirty. Do not revert changes you did not make.
   - If a touched file has unrelated edits, work around them carefully.
4. Verify.
   - Backend/code: run `./gradlew build` from `patient-api/` when Java/templates/CSS are changed.
   - UI: run locally and capture browser screenshots for changed screens when feasible.
   - Deployment: inspect Railway logs for the first real root cause, not repeated wrapper exceptions.
5. Record learning.
   - Add repeated failures or environment discoveries to `docs/memory.md`.
   - Add durable architectural decisions to `docs/decisions.md`.

## Local Run

Common local Docker MySQL setup used during development:

```bash
cd patient-api
PORT=8081 \
MYSQL_HOST=127.0.0.1 \
MYSQL_PORT=3307 \
MYSQL_DATABASE=clarix \
MYSQL_USER=clarix \
MYSQL_PASSWORD=clarixpass \
./gradlew bootRun
```

Standard local MySQL setup from README:

```bash
cd patient-api
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=clarix
export MYSQL_USER=clarix
export MYSQL_PASSWORD=clarix1234
./gradlew bootRun
```

Primary local URL:

```text
http://localhost:8081
```

## Deployment Notes

- Railway root directory must be `patient-api`.
- Railway must run the Spring Boot jar from `patient-api/build/libs`.
- Railway service port must align with Spring/Tomcat, currently `8080` in deployment logs unless `PORT` is supplied.
- Use one-line datasource values. Do not split JDBC query parameters into separate environment variables.
- Do not commit secrets. If a real API key appears in chat, screenshots, docs, or git history, rotate it.

## GitHub / Planning Workflow

When asked to plan larger work:

- `grill-me`: ask focused questions until the task is concrete.
- `to-prd`: turn the agreed direction into a PRD.
- `to-issues`: split the PRD into small vertical slices.
- `handoff`: summarize current state before long pauses or new sessions.
- `improve-codebase-architecture`: use after behavior is stable, not before.

Installed local skills may require restarting Codex or Claude Code before automatic discovery.

