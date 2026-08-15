# Current Status

Last updated: 2026-08-15

## State

Feature complete. T001-T012 passed final critical-feature closure review on 2026-08-15.

## Completed Tasks

- T001: architecture decision accepted and recorded in `plan.md`.
- T002: deterministic JVM coroutine support added with `MainDispatcherRule`.
- T003: cent-safe calculation contract tests added test-first.
- T004: `Money`, `Payment`, `SaleCalculation`, and `SaleCalculator` implemented; focused tests are green.
- T005: immutable `SaleSnapshot`, suspending repository boundary, Retrofit adapter, fake repository, and payload-capture tests implemented.
- T006: single-flight coordinator, confirmed-success reset/completion, active ViewModel integration, and in-flight UI guards implemented.
- T007: modeled retryable failure, exact-snapshot retry, preserved UI state, and actionable retry controls implemented.
- T008: unused duplicated `SalesViewModel` removed; `BarcodeViewModel` delegates submission to the sole authoritative coordinator.
- T009: Android/backend request DTOs aligned, redundant nested tenancy removed, Brasilia offset made explicit, and contract gates strengthened.
- T010: canonical documentation updated; review integrated cent-safe arithmetic into the production ViewModel and closed all critical/high findings.
- T011: all automated project gates passed; one blocking manifest lint issue was fixed and revalidated.
- T012: all requirements traced to passing evidence; recorded SDD run and physical-device smoke test passed.

## Key Decisions

- `SaleSubmissionCoordinator` will be the single authoritative workflow owner.
- `SalesRepository` is a suspending injected boundary; it must not launch a fire-and-forget coroutine.
- Submission creates an immutable `SaleSnapshot` before the first suspension.
- Retry reuses the same snapshot, timestamp, and local submission id.
- Submission is local single-flight; remote exactly-once behavior remains impossible without backend idempotency support.
- Business money uses non-negative `Long` cents.
- Decimal text and legacy `Double` enter through explicit validated conversions.
- Conversion back to the existing numeric DTO representation occurs only at the API boundary.

## Implemented Files

- `app/src/main/java/com/m4/red_android/sales/Money.kt`
- `app/src/main/java/com/m4/red_android/sales/SaleCalculator.kt`
- `app/src/test/java/com/m4/red_android/sales/SaleCalculatorTest.kt`
- `app/src/test/java/com/m4/red_android/testing/MainDispatcherRule.kt`
- `app/src/test/java/com/m4/red_android/testing/MainDispatcherRuleTest.kt`
- `app/src/main/java/com/m4/red_android/sales/SaleSnapshot.kt`
- `app/src/main/java/com/m4/red_android/data/repository/SalesRepository.kt`
- `app/src/main/java/com/m4/red_android/data/repository/RetrofitSalesRepository.kt`
- `app/src/test/java/com/m4/red_android/sales/FakeSalesRepository.kt`
- `app/src/test/java/com/m4/red_android/sales/SaleSnapshotTest.kt`
- `app/src/main/java/com/m4/red_android/sales/SaleSubmissionState.kt`
- `app/src/main/java/com/m4/red_android/sales/SaleSubmissionCoordinator.kt`
- `app/src/test/java/com/m4/red_android/sales/SaleSubmissionTest.kt`

## Verification Evidence

- Focused `SaleCalculatorTest`: passed on 2026-08-14.
- Focused `MainDispatcherRuleTest`: passed on 2026-08-14.
- Focused `SaleSnapshotTest`: passed on 2026-08-15.
- Focused sales package including `SaleSubmissionTest`: passed on 2026-08-15.
- Focused failure/retry `SaleSubmissionTest`: passed on 2026-08-15.
- Focused sales regression after ViewModel consolidation: passed on 2026-08-15.
- Android serialization contract test and 41 focused backend contract/route tests: passed on 2026-08-15.
- Android `contracts:check` and backend `openapi:check`: passed on 2026-08-15.
- Full Android JVM suite plus contract/SDD/diff checks after code review: passed on 2026-08-15.
- T011: 20 JVM tests passed; sales coverage 176/181 lines and 60/82 branches; lint, static analysis, coverage, and debug build passed on 2026-08-15.
- T012: recorded SDD run passed; APK installed/launched on Moto G35 Android 14 with no crash/ANR; final matrix recorded in `evidence.md`.
- Gradle result: `BUILD SUCCESSFUL`.
- `npm run sdd:check`: passed.
- `npm run contracts:check`: passed.
- `git diff --check`: passed.

Use the installed full JDK:

```bash
JAVA_HOME=/home/michel/.local/share/mise/installs/java/17.0.2 ./gradlew testDebugUnitTest
```

## Closure

Closed on 2026-08-15. No open critical/high finding remains. All requirements trace
to passing automated evidence; physical-device installation, launch, rendering, and
crash/ANR smoke checks also passed.

## Accepted Follow-up

- Add a backend idempotency key in a future public-contract feature to cover ambiguous
  timeout/process-death exactly-once semantics beyond local single-flight.
