# Final Evidence — Sale Submission Integrity

Date: 2026-08-15

## Release Decision

Passed. No open critical or high-severity finding remains. All six requirements
trace to focused passing tests, canonical documentation, and a successful recorded
SDD run.

## Requirement Traceability

| Requirement | Implementation evidence | Passing verification |
| --- | --- | --- |
| REQ-SALE-SUBMISSION-INTEGRITY-001 | `SaleSnapshot` copies items/payments; `SalesRepository` is the async boundary; `SalesRequest` maps only backend fields | `SaleSnapshotTest`, `RetrofitSalesRepositoryContractTest`, `contracts:check` |
| REQ-SALE-SUBMISSION-INTEGRITY-002 | Coordinator publishes reset/completion only after repository success | `confirmedSuccessResetsAndEmitsCompletionExactlyOnce` |
| REQ-SALE-SUBMISSION-INTEGRITY-003 | `Failed` retains the exact snapshot and exposes retry; UI state is not reset | `failurePreservesSnapshotAndRetrySubmitsItWithoutDuplicatingPayments` |
| REQ-SALE-SUBMISSION-INTEGRITY-004 | Mutex-serialized `Ready` → `Submitting` transition and guarded terminal transition | `concurrentSubmitAttemptsCreateOneRequestWhileFirstIsInFlight` |
| REQ-SALE-SUBMISSION-INTEGRITY-005 | Production ViewModel delegates totals, discount, balance, paid amount, and change to `Money`/`SaleCalculator` | Exact/partial/excess, discount, invalid input, legacy conversion, and overflow cases in `SaleCalculatorTest` |
| REQ-SALE-SUBMISSION-INTEGRITY-006 | Deterministic coroutine support and focused calculation, snapshot, coordinator, and serialization tests | 20 JVM tests passed; sales coverage 176/181 lines and 60/82 branches |

## Automated Gates

- Recorded run: `runs/2026-08-15T14-35-30-554Z.md` — passed.
- `npm run sdd:check` — passed.
- `npm run contracts:check` — passed.
- `npm run test` — passed.
- `npm run test:coverage` — passed; XML and HTML generated.
- `npm run lint` — passed.
- `npm run static:analysis` — passed.
- `npm run build` — passed; debug APK generated.
- Backend focused sale contract/route suite — 41 tests passed.
- Backend `npm run openapi:check` — passed.
- `git diff --check` in both repositories — passed.

## Physical Device Evidence

- Device: Motorola moto g35 5G, Android 14 / API 34.
- Transport: authorized USB ADB serial `ZF525GFC55`.
- APK: debug `versionCode=3`, `versionName=1.0`, installed successfully.
- Launch: `MainActivity` returned `Status: ok`, became the resumed activity, and
  rendered the POS/camera screen in approximately 1.8 seconds.
- Runtime: app process remained active; recent log inspection found no app crash or ANR.
- Slow response, offline failure/retry, and rapid concurrent actions are verified at
  the deterministic coordinator boundary. They were not repeated as live backend sales
  on the physical device because no isolated seeded sale account/product was available.

## Accepted Residual Risk

Local single-flight cannot guarantee server-side exactly-once behavior after an
ambiguous timeout or process death. End-to-end idempotency requires a future public
backend idempotency key and is outside this feature's contract-preserving scope.
