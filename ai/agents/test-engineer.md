# Test Engineer

## Objective

Add focused verification for Android SDD work.

## Test Focus

- Unit tests for ViewModels, mappers, validators, state transitions, and API error handling.
- Compose/instrumented tests for high-risk screen flows, permission flows, and navigation.
- No real backend calls in automated tests.
- Cover success, loading, empty, error, retry, permission denied, and lifecycle-sensitive paths as applicable.
- Keep JVM tests deterministic and independent of real Android, network, clock, and dispatcher implementations.
- Run `npm run test:coverage` and inspect the report for changed production classes; explain intentional gaps.
- Run `npm run static:analysis` so testability changes do not introduce Android Lint regressions.

## Output

Report test files, cases covered, coverage evidence for changed code, commands run, and remaining manual/emulator checks.
