# Clarix Vertical Slice Issues

Last updated: 2026-05-21

Use these as working issues. Each slice should leave the app buildable and demoable.

## Slice 1: Lock Mobile Auth To Figma Reference

Status: in progress

Scope:

- `landing.html`
- `auth/login.html`
- `auth/signup.html`
- `patient.css`

Acceptance:

- Splash, patient login, doctor login render at `390x844`.
- `./gradlew build` passes.
- Login/signup still preserve role flow.

## Slice 2: Finish Doctor Dashboard Reference UI

Status: pending

Scope:

- `doctor/index.html`
- `doctor/patient.html`
- `doctor.css`

Acceptance:

- Dark clinical layout aligns with supplied Figma Web UI references.
- Header contains primary menu and adjacent search.
- Grid overlays are absent.
- Patient cards, timeline, PHQ-9, medication, exercise, meal views are scannable.
- Desktop screenshot is verified.

## Slice 3: Calm Patient Emotion Logging

Status: pending

Scope:

- `patient/mood-pick.html`
- `patient/mood-journal.html`
- `patient/today.html`
- `patient.css`

Acceptance:

- Emotion colors feel calming and supportive.
- Severe/risk states remain distinguishable.
- Patient can record emotion in fewer steps.
- Mobile screenshot is verified.

## Slice 4: AI SOAP Error Transparency

Status: pending

Scope:

- `GeminiSoapService`
- `DoctorApiController`
- `doctor/patient.html`
- frontend JS handling SOAP response

Acceptance:

- Missing `GEMINI_API_KEY` returns explicit config error.
- Provider/network failures return explicit provider error.
- UI displays a concise Korean error message.
- No API key is logged.

## Slice 5: Railway Deployment Checklist

Status: pending

Scope:

- `README.md`
- `docs/실행방법.md`
- deployment docs
- optional healthcheck/config hardening

Acceptance:

- Railway root directory, start command, healthcheck, and MySQL env vars are documented.
- One-line `SPRING_DATASOURCE_URL` examples are included.
- Known bad examples are documented in `docs/memory.md`.

## Slice 6: Validation UX Completion

Status: in progress

Scope:

- form DTOs
- controllers
- templates with flash errors
- validation tests

Acceptance:

- Invalid form submissions do not crash.
- User sees a concise error.
- Validation tests cover representative forms.
- `./gradlew build` passes.

## Slice 7: Commit Stable Demo Checkpoint

Status: pending

Scope:

- Review diff.
- Run build.
- Run local smoke checks.
- Commit stable state.
- Push to remote when requested.

Acceptance:

- Commit message describes UI, validation, and harness docs clearly.
- No secrets are committed.
- Remote deployment can be triggered from the pushed commit.

