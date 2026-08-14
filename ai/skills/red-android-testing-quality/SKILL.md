---
name: red-android-testing-quality
description: Guidance for RED Android unit, Compose, instrumented, lint, Gradle build, and SDD verification evidence. Use when adding tests, choosing verification commands, recording SDD runs, or reviewing release readiness.
---

# RED Android Testing Quality

## Use When

- Adding or reviewing tests.
- Choosing verification commands for a feature plan.
- Recording evidence with `npm run sdd:run`.

## Workflow

1. Prefer fast unit tests for pure logic, ViewModel state, DTO mapping, and error handling.
2. Use Compose or instrumented tests for navigation, permissions, camera, or UI flows that unit tests cannot prove.
3. Mock backend calls; do not depend on live services.
4. Run the narrowest useful command first, then project gates.
5. Record evidence under `docs/features/{feature}/runs/` before closing high-impact work.

## Default Gates

- `npm run sdd:check`
- `npm run contracts:check`
- `npm run test`
- `npm run lint`
- `npm run build`
- `npm run connected:test` when a device/emulator check is required.
