# Sale ViewModel And Compose Verification Plan

## Files

- `BarcodeViewModel.kt`, payment/change composables, navigator, new JVM and androidTest fixtures/tests.

## Canonical Documentation

- Update `docs/specs/app.spec.md`, `docs/tasks/app.tasks.md`, and
  `docs/memory/project.memory.md` with presentation-state and lifecycle testing rules.

## Context Bundle

- SDD/mobile context, feature 0001 evidence, feature 0002 session seams, current sale UI/ViewModel.

## Agents

- `test-engineer`, `mobile-ux-regression-reviewer`, `implementation-engineer`, `code-reviewer`, `release-gate-reviewer`

## Skills

- `red-android-testing-quality`, `red-sdd-feature-closure`

## Implementation Sequence

1. Extract remaining test seams without behavior change.
2. Add ViewModel tests, then Compose/navigation tests.
3. Normalize presentation money formatting and lifecycle collection.
4. Run device matrix and release gates.

## Tests

- Focused test files:
  - `app/src/test/java/com/m4/red_android/viewmodels/BarcodeViewModelTest.kt`
  - `app/src/androidTest/java/com/m4/red_android/ui/scanner/PaymentFlowTest.kt`
  - `app/src/androidTest/java/com/m4/red_android/SaleNavigationTest.kt`
- Commands: `npm run sdd:check`, `npm run contracts:check`, `npm run test`,
  `npm run connected:test`, `npm run lint`, `npm run static:analysis`, `npm run build`.

## Gate Checks

- SDD, contracts, test, coverage, connected test, lint, static analysis, build.

## Risks

- Tests coupled to Compose internals, duplicate event collection, and flaky dispatcher/clock behavior.

## Definition Of Done

- All requirements covered with deterministic tests and physical-device evidence; no critical UI regression finding.
