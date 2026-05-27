# PRD: Clarix Demo Stabilization

Last updated: 2026-05-21

## Summary

Stabilize Clarix into a reliable demo product for patient self-reporting, clinician review, and AI-assisted SOAP summarization.

## Goals

- Match key Figma references for mobile auth and clinician screens.
- Keep patient workflows calm and fast.
- Make doctor workflows dense, scannable, and clinically useful.
- Make Railway deployment repeatable.
- Make AI SOAP failures actionable.
- Keep validation errors user-facing and safe.

## Non-Goals

- Full production compliance.
- Full EMR replacement.
- Multi-tenant billing.
- Native iOS/Android app.
- Replacing Thymeleaf with React.

## Users

- Patient: records mood, medication adherence, meal, exercise, and PHQ-9.
- Doctor: triages patient risk, reviews trends, writes SOAP, prescribes medication.
- Nurse/technician/reception/admin: support operational workflows.

## Requirements

### Mobile Auth

- Splash screen must match Figma direction: gradient background, large `Clarix`, bottom CTA.
- Patient login copy: `Sign in Clarix.`
- Doctor login copy: `Welcome to Clarix.`
- Inputs must be touch-friendly and visually consistent with Figma.

### Patient UI

- Mood/emotion entry should use calming color language.
- Daily workflow should prioritize the next action.
- Risk indicators should be visible but not visually aggressive unless clinically urgent.

### Doctor UI

- Patient list should show risk, PHQ-9, adherence, last record, and SOAP status.
- Patient detail should move toward the dark clinical Figma reference.
- Top navigation/search should be easy to scan.
- Charts should be readable without design grid overlays.

### AI SOAP

- Missing API key should return a clear configuration error.
- Provider/network failures should be distinguishable.
- UI should show a concise actionable error.

### Deployment

- Railway root directory must be `patient-api`.
- MySQL variables must be documented with exact format.
- Healthcheck must pass without requiring authenticated pages.
- Build and runtime logs must expose enough information to diagnose startup failure.

### Validation

- All form submissions should validate DTOs.
- User-facing errors should not expose stack traces.
- Tests should cover common invalid inputs and enum binding.

## Acceptance Criteria

- `./gradlew build` passes.
- Local app starts and serves:
  - `/`
  - `/auth/login?role=patient`
  - `/auth/login?role=doctor`
  - `/doctor/`
  - `/patient/`
- Figma-critical screens have screenshot checks.
- Railway deployment has documented environment variables.
- AI SOAP failure states are understandable without reading stack traces.

