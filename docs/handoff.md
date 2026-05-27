# Clarix Handoff

Last updated: 2026-05-21

## Current Goal

Stabilize Clarix as a demo-ready Spring Boot web app with:

- Patient and doctor UI separated clearly.
- Figma-aligned mobile auth/splash screens.
- Doctor dashboard/detail pages moving toward the supplied dark clinical reference.
- Reliable Railway + MySQL deployment.
- Validated form inputs with user-facing errors.
- AI SOAP generation that fails with explicit, diagnosable errors.

## Current State

- Main application lives in `patient-api/`.
- Build system: Gradle wrapper in `patient-api/`.
- Auth: Spring Security session login with demo accounts.
- DB: MySQL via Spring Data JPA/Hibernate.
- Demo seed: `DemoDataInitializer` preserves existing data when users already exist.
- Local app has been run on `http://127.0.0.1:8081`.
- Mobile auth/splash UI has been changed to match the Figma mobile reference more closely.
- Bean Validation has been added across many form DTOs and controllers, with `ValidationFeedback` for redirect flash errors.

## Important Recent Work

- Mobile landing/auth:
  - `templates/landing.html`
  - `templates/auth/login.html`
  - `templates/auth/signup.html`
  - `static/css/patient.css`
- Doctor reference UI work has touched:
  - `templates/doctor/index.html`
  - `templates/doctor/patient.html`
  - `static/css/doctor.css`
- Validation work has touched:
  - `dto/*Form.java`
  - `web/*Controller.java`
  - `web/ValidationFeedback.java`
  - template flash error areas
  - `src/test/java/com/clarix/validation/`

## Dirty Worktree Warning

The worktree has many uncommitted changes from UI, validation, and deployment stabilization work. Do not reset or revert broadly. Review `git status --short` and `git diff` before committing.

## Verification Known Good

Most recent successful command:

```bash
cd patient-api
./gradlew build
```

Recent local screenshot checks used:

```bash
npx playwright screenshot --viewport-size=390,844 http://127.0.0.1:8081/ /private/tmp/clarix-landing-figma-v2.png
npx playwright screenshot --viewport-size=390,844 'http://127.0.0.1:8081/auth/login?role=patient' /private/tmp/clarix-login-patient-figma-v2.png
npx playwright screenshot --viewport-size=390,844 'http://127.0.0.1:8081/auth/login?role=doctor' /private/tmp/clarix-login-doctor-figma-v2.png
```

## Next Recommended Slices

1. Finish Figma-aligned doctor dashboard and patient detail UI.
2. Improve patient emotion logging colors and interaction copy.
3. Make AI SOAP failures explicit in the UI and logs.
4. Lock Railway env var setup into documentation and deployment checks.
5. Commit current stable state once build and core screenshots pass.

## Demo Accounts

| Role | Email | Password |
|---|---|---|
| Admin | `admin@clarix.demo` | `clarix1234` |
| Doctor | `dr.kim@clarix.demo` | `clarix1234` |
| Nurse | `nurse@clarix.demo` | `clarix1234` |
| Technician | `tech@clarix.demo` | `clarix1234` |
| Reception | `desk@clarix.demo` | `clarix1234` |
| Patient 1 | `patient1@clarix.demo` | `clarix1234` |
| Patient 2 | `patient2@clarix.demo` | `clarix1234` |
| Patient 3 | `patient3@clarix.demo` | `clarix1234` |
| Patient 4 | `patient4@clarix.demo` | `clarix1234` |

