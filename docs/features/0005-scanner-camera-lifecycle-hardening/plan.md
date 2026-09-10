# Scanner Camera Lifecycle Hardening Plan

## Files

- Camera preview/reader, scanner screen/overlay, permission handler, BarcodeViewModel seam, new scanner domain/tests.

## Canonical Documentation

- Update `docs/specs/app.spec.md`, `docs/tasks/app.tasks.md`, and
  `docs/memory/project.memory.md` with camera ownership and permission policy.

## Context Bundle

- SDD/mobile context, feature 0003 fixtures, camera/permission source subset.

## Agents

- `android-architecture-engineer`, `mobile-ux-regression-reviewer`, `test-engineer`, `implementation-engineer`, `code-reviewer`, `release-gate-reviewer`

## Skills

- `red-android-camera-barcode`, `red-android-testing-quality`, `red-sdd-feature-closure`

## Implementation Sequence

1. Characterize lifecycle, permission, dedup, and resource ownership.
2. Extract scanner controller/resource seams.
3. Implement lifecycle and permission state model.
4. Add Compose/device regression matrix.
5. Update docs and release gates.

## Tests

- Focused files include scanner policy/resource JVM tests and permission/lifecycle
  Compose tests under `app/src/test/java/.../scanner/` and `app/src/androidTest/java/.../scanner/`.
- Commands: `npm run sdd:check`, `npm run contracts:check`, `npm run test`,
  `npm run connected:test`, `npm run lint`, `npm run static:analysis`, `npm run build`.

## Gate Checks

- SDD, contracts, test, connected test, lint, static analysis, build, device evidence.

## Risks

- Vendor-specific camera behavior, flaky instrumentation, lifecycle races, and accidental scan suppression.

## Definition Of Done

- Camera/permission/resource invariants pass tests on API matrix and physical device with no leak/high finding.
