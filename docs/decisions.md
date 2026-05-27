# Clarix Decisions

Last updated: 2026-05-21

## 1. Keep Spring Boot + Thymeleaf for Demo

Clarix remains a Spring Boot MVC app with Thymeleaf templates rather than splitting into a separate SPA.

Reason:

- Current app already has Spring Security sessions, JPA services, and server-rendered pages.
- Demo speed matters more than introducing a separate frontend runtime.
- Thymeleaf keeps deployment simple on Railway.

## 2. Use MySQL 8 With Hibernate `ddl-auto=update` for Demo

The app uses MySQL and lets Hibernate update schema during demo development.

Reason:

- Course/project requirement includes MySQL CRUD.
- Demo data can be seeded automatically.
- Formal migrations add process overhead for the current demo stage.

Tradeoff:

- For production, replace `ddl-auto=update` with Flyway/Liquibase migrations and backups.

## 3. Separate Patient and Clinician UI Directions

Patient UI should be calm, mobile-first, and guided.

Clinician UI should be dense, scan-friendly, and work-focused.

Reason:

- Patient workflows need low-friction daily input.
- Doctor workflows need risk triage, comparison, and repeated action.
- One visual language for both roles creates poor ergonomics.

## 4. Keep Demo Seed Data Until Real Onboarding Is Stable

`DemoDataInitializer` remains active and skips seeding when users already exist.

Reason:

- Demo reliability depends on predictable accounts and populated charts.
- Existing user data should not be overwritten.

## 5. AI SOAP Must Fail Explicitly

AI SOAP should not hide configuration/provider errors behind a generic 503.

Reason:

- Demo debugging needs fast diagnosis.
- Missing API key, provider rejection, and network failure require different fixes.

## 6. Harness Workflow Is Project Policy

Major work should follow:

```text
deep interview -> PRD -> vertical slices -> implementation -> verification -> memory update
```

Reason:

- The project has multiple moving parts: UI, backend, DB, deployment, external AI.
- Repeated environment mistakes cost more than adding lightweight operating docs.

