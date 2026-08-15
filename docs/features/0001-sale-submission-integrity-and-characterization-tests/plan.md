# Sale Submission Integrity And Characterization Tests Plan

## Files

- `app/src/main/java/com/m4/red_android/viewmodels/BarcodeViewModel.kt`
- `app/src/main/java/com/m4/red_android/viewmodels/SalesViewModel.kt`
- `app/src/main/java/com/m4/red_android/data/models/Sales.kt`
- `app/src/main/java/com/m4/red_android/ui/scanner/PaymentInputCard.kt`
- `app/src/main/java/com/m4/red_android/sales/SaleCalculator.kt`
- `app/src/main/java/com/m4/red_android/sales/SaleSubmissionCoordinator.kt`
- `app/src/main/java/com/m4/red_android/sales/SaleSubmissionState.kt`
- `app/src/main/java/com/m4/red_android/sales/Money.kt`
- `app/src/main/java/com/m4/red_android/sales/SaleSnapshot.kt`
- `app/src/main/java/com/m4/red_android/data/repository/SalesRepository.kt`
- `app/src/main/java/com/m4/red_android/data/repository/RetrofitSalesRepository.kt`
- `app/src/test/java/com/m4/red_android/sales/SaleCalculatorTest.kt`
- `app/src/test/java/com/m4/red_android/sales/SaleSubmissionTest.kt`
- `app/src/test/java/com/m4/red_android/sales/FakeSalesRepository.kt`
- `app/src/test/java/com/m4/red_android/testing/MainDispatcherRule.kt`
- `app/src/test/java/com/m4/red_android/testing/MainDispatcherRuleTest.kt`
- `app/build.gradle.kts`

## Canonical Documentation

- Update `docs/specs/app.spec.md` with the confirmed-success and failure-preservation rules.
- Update `docs/tasks/app.tasks.md` with traceability to this feature.
- Update `docs/memory/project.memory.md` with the durable sale-submission invariant and chosen money representation.

## Context Bundle

- `docs/sdd/constitution.md`
- `docs/sdd/workflow.md`
- `docs/sdd/context-map.md`
- `docs/sdd/quality-gates.md`
- `ai/context/mobile.md`
- `ai/agents/android-architecture-engineer.md`
- `ai/agents/test-engineer.md`
- `ai/skills/red-android-testing-quality/SKILL.md`
- `ai/skills/red-android-backend-contract/SKILL.md`
- `docs/features/0001-sale-submission-integrity-and-characterization-tests/spec.md`
- `docs/features/0001-sale-submission-integrity-and-characterization-tests/tasks.md`
- the files listed in the Files section, loading only the subset needed per task

## Agents

- `android-architecture-engineer` validates the submission boundary, state machine, and cent-safe representation before implementation.
- `test-engineer` creates characterization and failure/retry tests before behavior changes.
- `implementation-engineer` implements one approved `Txxx` task at a time.
- `backend-contract-reviewer` confirms DTO compatibility with `POST /sales`.
- `code-reviewer` checks concurrency, state preservation, and regression risk.
- `release-gate-reviewer` validates evidence and canonical documentation before closure.

## Skills

- `red-android-testing-quality`
- `red-android-backend-contract`
- `red-sdd-feature-closure`

## Architecture Decision

Status: accepted on 2026-08-14 by `android-architecture-engineer` for T001.

### Decision Drivers

- A sale must not disappear locally before the backend confirms it.
- The request payload must not share mutable collections with UI/cart state.
- A slow request, rapid taps, failure, and retry must have deterministic behavior.
- Business calculations must be exact to the cent while the existing backend JSON contract remains compatible.
- This phase must create test seams without introducing broad modularization or a dependency-injection framework.

### Submission Ownership And Boundary

- One Kotlin-only `SaleSubmissionCoordinator` is the authoritative owner of validation, snapshot creation, submission transitions, retry, and single-flight behavior. The feature ViewModel owns the lifecycle coroutine and adapts coordinator state/effects for Compose; it does not implement a second workflow.
- `BarcodeViewModel` is the transitional UI adapter because it is the implementation used by `AppNavigator`. Until consolidation in T008, `SalesViewModel` must delegate to the same coordinator rather than copy its behavior.
- The coordinator depends on an injected `SalesRepository` and `Clock`. Neither coordinator nor ViewModel calls the global `RetrofitClient` or system time directly.
- The repository contract is suspending and does not launch its own coroutine:

```kotlin
interface SalesRepository {
    suspend fun submit(snapshot: SaleSnapshot): SaleSubmissionResult
}
```

- The ViewModel launches a lifecycle-owned child coroutine; the coordinator awaits the repository result and performs the corresponding guarded state transition.
- Retrofit and DTO mapping remain implementation details of `RetrofitSalesRepository`.

Alternatives rejected:

- Keeping `postSale()` fire-and-forget was rejected because its caller cannot order reset/navigation after the response.
- Letting composables call Retrofit was rejected because it couples network lifetime to recomposition and removes the JVM test seam.
- Introducing Hilt or new Gradle modules now was rejected as unnecessary scope for the first integrity slice.
- Keeping workflow ownership independently in each ViewModel was rejected because the existing duplication is itself a regression source.

### Immutable Snapshot

- Final submit first validates the current draft and then creates a `SaleSnapshot` containing immutable scalar values plus immutable `List` values for items and payments.
- Snapshot creation copies every collection with `map`/`toList`; the snapshot and its element types expose only `val` properties and no `MutableList` references.
- The realization timestamp, vendor, sale code, discount, and change are captured once during snapshot creation. Retry reuses that exact failed snapshot rather than rebuilding it from potentially changed UI state.
- `RetrofitSalesRepository` maps the snapshot to a new API DTO. The DTO must not retain references to mutable ViewModel collections.
- Resetting UI/cart state can therefore never mutate a request already passed to the repository.

Alternatives rejected:

- A shallow cast such as `_items as List<Item>` was rejected because it changes only the static type and retains the mutable backing list.
- Rebuilding the payload on retry was rejected because it can duplicate or change payments after an ambiguous failure.

### Submission State, Success, Failure, And Retry

The authoritative state machine is:

```text
Ready --Submit(valid)--> Submitting(snapshot)
Submitting --confirmed success--> Succeeded
Submitting --failure--> Failed(snapshot, error)
Failed --Retry--> Submitting(the same snapshot)
Succeeded --completion consumed--> Ready(empty)
```

Invariants:

- Invalid drafts remain `Ready` with an observable validation error and make no repository call.
- `Submitting` retains the snapshot, exposes progress, and rejects cart/payment mutation that could imply a second sale.
- Confirmed repository success is the only transition allowed to clear cart/payment state and emit the completion/navigation effect.
- Reset and completion effect are each produced once on the successful submission path.
- Failure preserves both the visible cart/payment state and the failed snapshot, emits no success/navigation effect, and exposes an actionable error plus retry.
- Retry submits the retained snapshot without appending payment entries again.
- Errors are modeled values rather than `printStackTrace()` control flow. Technical causes may be logged safely, while UI receives a stable message/category.

### Concurrency And Idempotency

- Submission transitions are serialized by the coordinator. A short `Mutex`-guarded check-and-set occurs before the first suspension; the mutex is not held during network I/O. If state is already `Submitting` or the snapshot already succeeded, subsequent submit/retry actions are ignored.
- The ViewModel owns the active lifecycle `Job`; the coordinator owns an attempt token so only the current attempt may publish a terminal transition. Tests must prove that rapid/concurrent actions result in one repository invocation and one completion effect.
- The final action remains disabled while `Submitting`; this is a UX reinforcement, not the correctness mechanism.
- A locally generated `submissionId` identifies a snapshot for logs, state correlation, and tests. It is stable across retry.
- Local single-flight prevents duplicate requests from rapid taps in one running client. It cannot guarantee server-side exactly-once behavior when a request times out after the backend may already have committed it, or after process death. True end-to-end idempotency would require a backend idempotency key and is explicitly deferred because this feature cannot change the public API contract.

Alternatives rejected:

- Button disabling alone was rejected because UI events can race or be invoked outside that button.
- A global mutex/singleton was rejected because submission ownership belongs to the feature coordinator instance and global state complicates lifecycle and tests.

### Monetary Representation And Validation

- Business calculations use a `Money` value object backed by signed `Long` cents. `Double` is not used for addition, subtraction, comparison, balance, discount, paid total, or change.
- User input is parsed as decimal text, accepting the supported dot/comma separator and at most two fractional digits. Blank, malformed, non-finite, zero, or negative payment values are rejected before constructing `Money`.
- Existing catalog/API `Double` values are converted with `BigDecimal.valueOf(value)`, validated as finite/non-negative, and scaled to two decimal places with `RoundingMode.UNNECESSARY`; unexpected fractional cents are rejected rather than rounded silently. No `BigDecimal(Double)` constructor is allowed.
- Arithmetic checks overflow. Discount must be non-negative and cannot exceed the sale total. Each payment requires a non-null method and positive amount. Excess payment is valid and becomes change; balance never becomes a negative exposed amount.
- At the API boundary only, cents are converted through `BigDecimal.valueOf(cents, 2)` to the numeric representation required by the existing `POST /sales` DTO. Contract tests must verify the resulting JSON values.

Alternatives rejected:

- Continuing with `Double` plus rounding was rejected because rounding after binary arithmetic does not preserve financial invariants.
- Using `BigDecimal` throughout UI state was rejected for this scale because `Long` cents provides simpler equality, serialization tests, and arithmetic; `BigDecimal` remains the explicit parsing/mapping boundary.

### Consequences And Follow-up

- T002 establishes deterministic `Dispatchers.Main` support. A meaningful fake repository cannot precede its production contract, so T005 creates the fake and payload tests; T006 owns state/single-flight tests and T007 owns failure/retry tests.
- T005 introduces the repository and DTO mapper; T006/T007 implement the accepted state machine.
- The current `Sales` DTO may remain temporarily for wire compatibility, but mutable/nullable lists cannot cross into `SaleSnapshot`.
- UI restructuring is limited to exposing submission progress/error and preventing repeated finalization; broader stateless Compose refactoring remains out of scope.
- No unresolved architecture or public-contract decision blocks T002. Backend-supported idempotency remains a documented residual risk for a future contract feature.
- T009 confirmed that `amountPaid` is a required numeric array and is represented by
  non-null `List<Double>` at the API boundary. Local sale/item codes are retained only
  in the immutable snapshot because the backend contract does not consume them.
  `realizedAt` is serialized with the explicit `America/Sao_Paulo` offset. Item-level
  `companyId` was removed from validation because authenticated root sale tenancy is
  authoritative and is already used by inventory processing.

## Implementation Sequence

1. Record the architecture decision for submission ownership, immutable snapshots, retry semantics, and money representation.
2. Add the minimum JVM test dependency and deterministic coroutine test support.
3. Add characterization tests for calculation; add the fake repository and payload capture alongside the repository boundary in step 5.
4. Introduce cent-safe pure calculation/validation without changing the backend JSON contract.
5. Introduce the repository boundary and explicit submission state.
6. Make submission single-flight; await backend success before resetting or emitting completion.
7. Preserve all sale state on failure and implement safe retry.
8. Resolve the duplicated ViewModel behavior with the smallest safe consolidation supported by tests.
9. Update canonical documentation and run all gates.

## Tests

- Focused test files:
  - `app/src/test/java/com/m4/red_android/sales/SaleCalculatorTest.kt`
  - `app/src/test/java/com/m4/red_android/sales/SaleSubmissionTest.kt`
  - `app/src/test/java/com/m4/red_android/sales/FakeSalesRepository.kt`
- Cases:
  - immutable payload remains populated after UI state reset;
  - success resets and emits completion once;
  - failure preserves state and emits retryable error;
  - retry succeeds without duplicate payments;
  - repeated in-flight submit is ignored;
  - exact, partial, and excess payments calculate cents correctly;
  - invalid method, amount, discount, and non-finite values are rejected.
- Commands:
  - `npm run sdd:check`
  - `npm run contracts:check`
  - `npm run test`
  - `npm run test:coverage`
  - `npm run lint`
  - `npm run static:analysis`
  - `npm run build`

## Gate Checks

- `npm run sdd:check`
- `npm run contracts:check`
- `npm run test`
- `npm run test:coverage`
- `npm run lint`
- `npm run static:analysis`
- `npm run build`
- Manual slow/offline/retry and rapid-tap checks recorded in the feature run evidence.

## Risks

- Backend DTO rounding drift is mitigated by explicit cent-to-number mapping and a Gson contract test.
- Mutable collection leakage is mitigated by copied immutable snapshot item/payment values.
- Concurrent local events are mitigated by the coordinator's mutex-guarded single-flight transition.
- ViewModel divergence was eliminated by removing the unused duplicate `SalesViewModel`.
- Accepted residual risk: ambiguous timeouts or process death can still duplicate a server-side sale until the backend supports an idempotency key.

## Definition Of Done

- Every `REQ-SALE-SUBMISSION-INTEGRITY-*` requirement has focused automated coverage.
- Backend failure or timeout preserves the complete sale state and exposes retry.
- Confirmed success resets state and emits completion exactly once.
- Only one submission can be active at a time.
- Calculation is cent-safe and DTO compatibility is verified.
- Canonical documentation and feature tasks are updated.
- All gate commands pass with evidence recorded by `npm run sdd:run -- 0001-sale-submission-integrity-and-characterization-tests`.
