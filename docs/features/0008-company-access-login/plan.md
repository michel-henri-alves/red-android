# Company Access Login — Execution Plan

**Status:** implemented locally — release verification pending

## Files

- `app/src/main/java/com/m4/red_android/ui/login/LoginScreen.kt`
- `app/src/main/java/com/m4/red_android/viewmodels/AuthViewModel.kt`
- `app/src/main/java/com/m4/red_android/AuthenticatedApp.kt`
- `app/src/main/java/com/m4/red_android/auth/SessionManager.kt`
- `app/src/main/java/com/m4/red_android/auth/ (new CompanyContextStore)`
- `app/src/main/java/com/m4/red_android/data/api/LoginApi.kt`
- `app/src/main/java/com/m4/red_android/data/api/RetrofitClient.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/ (backup exclusion rules)`
- `app/src/test/ and app/src/androidTest/ (company lifecycle coverage)`

Paths described as new directories or patterns are planned additions; confirm exact filenames before implementation. Preserve unrelated workspace edits.

## Canonical Documentation

docs/specs/app.spec.md, docs/specs/backend-contract.spec.md, docs/tasks/app.tasks.md, docs/tasks/backend-contract.tasks.md, docs/memory/project.memory.md. Update after implementation, distinguishing intended behavior from verified behavior.

## Implementation Sequence

1. Inspect existing session/outbox lifecycle; add fake-store/ViewModel tests for first login, failed login, mandatory change, mismatch, revalidation and late request results.
2. Add public Retrofit resolver and a separate persistent CompanyContextStore containing companyId/accessName/name/schemaVersion; exclude from backup on supported Android versions.
3. Implement setup, confirmation and login states with loading/error/retry; persist atomically only after authenticated response companyId matches resolved selection.
4. Restore confirmed context on restart/logout/expiry; revalidate before unauthenticated login/recovery; explicit switch clears transient secrets and rejects stale responses while preserving previous durable selection until success.
5. Handle upgrade: keep valid existing sessions; users without a saved accessName perform setup on next signed-out login. Do not reinterpret session companyId as an access name.
6. Run unit, contract, Compose/emulator and build gates; verify process kill, restart, logout, outbox protection and backup exclusions; update app/backend-contract specs, tasks and memory.

## Dependencies

Ecosystem plan owns rollout order. Backend contract and database mapping/index preparation precede consumer activation. Web activation also requires DNS/TLS/CORS readiness. Tests may start against the agreed contract before producer deployment.

## Agents

Task Agent fields identify execution roles, not a requirement to launch parallel agents. Implementation engineer, test engineer, API/backend contract reviewer, security tenant-isolation reviewer, code reviewer; infrastructure/data owner where applicable.

## Tests

- `app/src/test/java/com/m4/red_android/viewmodels/AuthViewModelTest.kt`
- `app/src/test/java/com/m4/red_android/auth/SessionManagerTest.kt`
- Planned new: `app/src/test/java/com/m4/red_android/auth/CompanyContextStoreTest.kt`
- Planned new: `app/src/androidTest/java/com/m4/red_android/CompanyAccessFlowTest.kt`

Run from `red-android`: npm run sdd:check; npm run contracts:check; npm run test; npm run lint; npm run build; npm run connected:test.

Focused checks must include assigned acceptance scenarios from spec.md. Record exact focused commands and results in local `runs/` during execution. Use disposable data/staging for migration and deployment tests.

## Risks

Collision or incorrect company mapping can misdirect login UX; validation and token tenant checks remain mandatory. Stale asynchronous work can change selection; cancel/sequence requests. Partial deployment can strand consumers; retain old API contracts and activate clients after dependencies. Rollback keeps assigned names/index and persisted data; revert consumer activation first.

## Gate Checks

- Every requirement maps to tasks and verification.
- No unresolved product blocker; production domain and company mapping recorded before rollout.
- API contracts agree across consumers and producer.
- Canonical docs and tenant/security review completed before closure.

## Definition Of Done

Assigned requirements implemented, relevant gates passed, evidence recorded, canonical documentation updated, review findings resolved or explicitly accepted. This scaffold alone does not satisfy runtime completion.

## Implemented file refinements and release boundary

See the project diff and canonical docs for actual paths. Company resolution has dedicated service/controller/middleware and API DTOs. Database index migration is 0006 with separate operator mapping script; Android CompanyAccessApi has its own response DTO, FileCompanyContextStore uses noBackupFilesDir, and protected navigation uses a scoped ViewModelStore. Backend CORS belongs to config/companyCors.js; optional infrastructure inputs are documented in the rollout runbook.

Record actual local verification before task closure; current cross-project evidence is in ECO-0002 verification.md. Company mappings and integrated rollout remain pending. Do not interpret local implementation as a completed release.


## ECO-T009 — tipo.click (2026-09-11)

The definitive application base domain is `tipo.click`, registered in AWS. Web entry is `https://<accessName>.tipo.click`; the apex gives company-access guidance. The API endpoint remains `https://7700ezljb5.execute-api.us-east-1.amazonaws.com`, including Android. Domain rollout must add no charges beyond registration/renewal. The existing Route53 zone has been associated with the active CloudFront Free plan; publication costs, certificate, reviewed company mappings and integrated acceptance remain pending. See the [domain migration subtask](../../../../docs/features/ECO-0002-company-access-login/domain-migration.md).
