# Clarix Memory

Last updated: 2026-05-21

This file stores recurring failures, environment facts, and lessons that future agents should not rediscover.

## Railway Deployment

- Root directory must be `patient-api`. If Railway analyzes repo root, it sees mostly docs and misses `build.gradle`/`gradlew`.
- Early jar start failures came from a bad jar path such as `*/build/libs/*jar` when the root directory or Docker/Railpack context was wrong.
- `secret ID missing for "" environment variable` was caused by an invalid env var name, including a leading-space variant like ` GEMINI_API_KEY`. Railway/BuildKit treats that as an empty secret ID and build fails before Java compilation.
- Do not use env var names with whitespace. Recreate the variable instead of renaming visually.
- Railway trial/build outages can cause unrelated deployment failures. Check Railway incident banner before changing app code.

## Railway MySQL

- MySQL connection failures showed:
  - `Communications link failure`
  - `java.net.ConnectException: Connection refused`
  - Hibernate failing while opening JDBC connection for DDL.
- Likely causes seen:
  - App connected to wrong host/port.
  - JDBC URL split across env vars.
  - `SPRING_DATASOURCE_URL` missing database name.
  - Query parameters accidentally stored as a separate `useSSL` env var.
  - Public proxy host used incorrectly or MySQL service changes not deployed.
- The datasource URL must be one line:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:<port>/<database>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

- Use Railway references only if Railway resolves them inside the app service. If references are unreliable, copy concrete host, port, database, user, and password from the MySQL service variables.
- Do not include wrapping quotes in Railway variable values.
- Do not split `serverTimezone=Asia/Seoul`; `AsiaSeoul` is invalid.

## AI SOAP / Gemini

- `/doctor/patient/{id}/llm-soap` previously returned `503 Service Unavailable`.
- Causes to check:
  - Missing or malformed `GEMINI_API_KEY`.
  - Key pasted with leading/trailing spaces or quotes.
  - External provider/network failure.
  - App hiding provider error behind generic 503.
- Do not log or commit actual API keys. If an API key was exposed in chat or screenshots, rotate it.
- UI should show explicit configuration/provider errors instead of only `503`.

## UI

- Figma mobile auth reference requires:
  - Soft vertical gradient: light blue to lavender to pale yellow/off-white.
  - White `Clarix` wordmark splash.
  - Transparent white-bordered inputs.
  - Lavender primary button.
  - Separate patient and doctor login copy.
- Current mobile auth has been changed in templates and `patient.css`.
- Doctor dashboard is not yet fully converted to the supplied dark Figma reference. Do not claim it is complete until screenshots match.
- Remove design grid overlays from shipped UI. Grids are for Figma/reference only.
- Patient emotion logging should use calming colors, not harsh warning colors unless conveying risk.

## Validation

- `spring-boot-starter-validation` is present.
- DTO-level Bean Validation has been added to many forms.
- Controllers should pair `@Valid` with `BindingResult` and redirect with a concise flash error.
- Enum form fields should bind directly to enum types when possible, not rely on repeated manual `valueOf`.
- Validation tests live under `src/test/java/com/clarix/validation/`.

## Local Development

- Local app commonly runs on `8081`.
- A local Docker MySQL has been used at `127.0.0.1:3307` with:
  - database `clarix`
  - user `clarix`
  - password `clarixpass`
- README also documents a standard MySQL `localhost:3306` setup with password `clarix1234`.
- Before starting a new server, check for existing Gradle/Java processes.
- 2026-05-27: Homebrew MySQL failed to start locally because `mysqld` could not load an `abseil` dylib from the installed MySQL 9.3 package. Docker was also not running. Reinstall/relink MySQL dependencies or use Docker MySQL before attempting live Spring screenshots.
- 2026-06-01: local disk had only about 130-148MiB free. Gradle reached `classes`, but `bootJar` and daemon cache writes failed with `No space left on device`; free disk space before claiming full `./gradlew build` verification.
