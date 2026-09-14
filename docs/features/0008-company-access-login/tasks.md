# Company Access Login — Tasks

**Release evidence (2026-09-14):** deployed and verified; see [verification](runs/verification-2026-09-14.md). Broad acceptance items below remain open where real-account, accessibility/backup or complete IAM-plan evidence is still required.


- [ ] T001 - REQ-ECO-002, REQ-ECO-004, REQ-ECO-005, REQ-ECO-006, REQ-ECO-007, REQ-ECO-010, REQ-ECO-011 Inspect existing session/outbox lifecycle; add fake-store/ViewModel tests for first login, failed login, mandatory change, mismatch, revalidation and late request results.
  - Agent: `test-engineer`
  - Depends on: none
  - Verification: `./gradlew testDebugUnitTest --tests "*AuthViewModelTest" --tests "*SessionManagerTest" --tests "*CompanyContextStoreTest"`; lifecycle UI checks via `npm run connected:test`; record results in runs/ (new test paths implemented in this feature)

- [ ] T002 - REQ-ECO-002, REQ-ECO-004, REQ-ECO-005, REQ-ECO-006, REQ-ECO-007, REQ-ECO-010, REQ-ECO-011 Add public Retrofit resolver and a separate persistent CompanyContextStore containing companyId/accessName/name/schemaVersion; exclude from backup on supported Android versions.
  - Agent: `implementation-engineer`
  - Depends on: T001
  - Verification: `./gradlew testDebugUnitTest --tests "*AuthViewModelTest" --tests "*SessionManagerTest" --tests "*CompanyContextStoreTest"`; lifecycle UI checks via `npm run connected:test`; record results in runs/ (new test paths implemented in this feature)

- [ ] T003 - REQ-ECO-002, REQ-ECO-004, REQ-ECO-005, REQ-ECO-006, REQ-ECO-007, REQ-ECO-010, REQ-ECO-011 Implement setup, confirmation and login states with loading/error/retry; persist atomically only after authenticated response companyId matches resolved selection.
  - Agent: `implementation-engineer`
  - Depends on: T002
  - Verification: `./gradlew testDebugUnitTest --tests "*AuthViewModelTest" --tests "*SessionManagerTest" --tests "*CompanyContextStoreTest"`; lifecycle UI checks via `npm run connected:test`; record results in runs/ (new test paths implemented in this feature)

- [ ] T004 - REQ-ECO-002, REQ-ECO-004, REQ-ECO-005, REQ-ECO-006, REQ-ECO-007, REQ-ECO-010, REQ-ECO-011 Restore confirmed context on restart/logout/expiry; revalidate before unauthenticated login/recovery; explicit switch clears transient secrets and rejects stale responses while preserving previous durable selection until success.
  - Agent: `implementation-engineer`
  - Depends on: T003
  - Verification: `./gradlew testDebugUnitTest --tests "*AuthViewModelTest" --tests "*SessionManagerTest" --tests "*CompanyContextStoreTest"`; lifecycle UI checks via `npm run connected:test`; record results in runs/ (new test paths implemented in this feature)

- [ ] T005 - REQ-ECO-002, REQ-ECO-004, REQ-ECO-005, REQ-ECO-006, REQ-ECO-007, REQ-ECO-010, REQ-ECO-011 Handle upgrade: keep valid existing sessions; users without a saved accessName perform setup on next signed-out login. Do not reinterpret session companyId as an access name.
  - Agent: `implementation-engineer`
  - Depends on: T004
  - Verification: `./gradlew testDebugUnitTest --tests "*AuthViewModelTest" --tests "*SessionManagerTest" --tests "*CompanyContextStoreTest"`; lifecycle UI checks via `npm run connected:test`; record results in runs/ (new test paths implemented in this feature)

- [ ] T006 - REQ-ECO-002, REQ-ECO-004, REQ-ECO-005, REQ-ECO-006, REQ-ECO-007, REQ-ECO-010, REQ-ECO-011 Run unit, contract, Compose/emulator and build gates; verify process kill, restart, logout, outbox protection and backup exclusions; update app/backend-contract specs, tasks and memory.
  - Agent: `implementation-engineer`
  - Depends on: T005
  - Verification: npm run sdd:check; npm run contracts:check; npm run test; npm run lint; npm run build; npm run connected:test

- [ ] T007 - REQ-ECO-002, REQ-ECO-004, REQ-ECO-005, REQ-ECO-006, REQ-ECO-007, REQ-ECO-010, REQ-ECO-011 Review completed change, tenant isolation, compatibility and canonical documentation.
  - Agent: `code-reviewer`
  - Depends on: T006
  - Verification: findings resolved or documented with owner; local evidence linked to ECO-0002 verification.md
