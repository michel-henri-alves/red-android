# Durable Sale Outbox And Idempotency Plan

## Files

- New Android sale outbox database/entity/DAO/repository/worker/UI/tests.
- `SaleSubmissionCoordinator` integration.
- Backend sale entity/validation/service/repository/OpenAPI/tests.

## Canonical Documentation

- Update Android `docs/specs/app.spec.md`, `docs/tasks/app.tasks.md`, and
  `docs/memory/project.memory.md`; update backend sale specs/tasks/memory and OpenAPI.

## Context Bundle

- Features 0001–0003, both project SDD contexts, backend sales/auth/tenant sources.

## Agents

- `android-architecture-engineer`, `backend-contract-reviewer`, `security-tenant-isolation-reviewer`, `test-engineer`, `implementation-engineer`, `release-gate-reviewer`

## Skills

- `red-android-backend-contract`, `red-android-testing-quality`, `red-sdd-feature-closure`

## Implementation Sequence

1. Approve cross-repository idempotency/outbox architecture and threat model.
2. Implement backend atomic idempotency contract test-first.
3. Implement Room schema/migration and outbox reducer test-first.
4. Add WorkManager submission/reconciliation.
5. Integrate status UI and process-death/device scenarios.
6. Update contracts/docs and run both projects' release gates.

## Tests

- Focused test files:
  - `app/src/test/java/com/m4/red_android/sales/outbox/SaleOutboxRepositoryTest.kt`
  - `app/src/androidTest/java/com/m4/red_android/sales/outbox/SaleOutboxMigrationTest.kt`
  - `app/src/test/java/com/m4/red_android/sales/outbox/SaleSubmissionWorkerTest.kt`
- Backend service/repository/route and Android contract/device tests.
- Commands: `npm run sdd:check`, `npm run contracts:check`, `npm run test`,
  `npm run test:coverage`, `npm run connected:test`, `npm run lint`,
  `npm run static:analysis`, `npm run build`.

## Gate Checks

- Full Android and backend SDD, contract/OpenAPI, test/coverage, lint, static analysis, build, migration, and device gates.

## Risks

- Duplicate financial writes, migration data loss, cross-tenant key collisions, worker races, and misleading offline UX.

## Definition Of Done

- Process-death/offline/ambiguous-timeout scenarios pass end-to-end with no duplicate sale; migrations and tenant isolation are proven.
