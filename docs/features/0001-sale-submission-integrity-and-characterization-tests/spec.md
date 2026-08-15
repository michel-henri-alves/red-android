# Sale Submission Integrity And Characterization Tests Spec

## Problem

The POS currently clears the cart and reports completion before `POST /sales` is confirmed. Because the request is launched asynchronously with references to mutable lists, a slow or failed request can receive an emptied payload, lose the cashier's working state, or produce a false-success navigation event.

## Scope

- In scope:
  - characterize current cart, payment, and sale-submission behavior with focused JVM tests;
  - create an immutable sale snapshot before crossing the asynchronous boundary;
  - expose observable submitting, success, and retryable-error states;
  - reset cart/payment state and navigate only after confirmed backend success;
  - preserve state after network/server failure and support safe retry;
  - suppress repeated submission while a request is in flight;
  - validate payment method, amounts, discount, balance, and change with cent-safe arithmetic;
  - prevent the duplicated `BarcodeViewModel` and `SalesViewModel` implementations from diverging during this change.
- Out of scope:
  - camera lifecycle or permission refactoring;
  - product-creation refactoring;
  - authentication implementation;
  - broad package or Gradle module reorganization;
  - changes to the public backend contract;
  - general Compose navigation modernization.

## Impact Classification

- Impact: high
- Creates new domain/workflow: no
- Changes domain model: yes
- Changes public API contract: no
- Changes durable architecture/project memory: yes
- Canonical docs required: `docs/specs/app.spec.md`, `docs/tasks/app.tasks.md`, and `docs/memory/project.memory.md`

## Criticality

Critical. This feature changes sales/payment integrity and the conditions under which a completed sale is reported to the cashier.

## Requirements

- REQ-SALE-SUBMISSION-INTEGRITY-001: Sale submission must send an immutable snapshot containing every cart item, payment entry, discount, change, vendor, code, and realization timestamp present when submission begins.
- REQ-SALE-SUBMISSION-INTEGRITY-002: Cart/payment state must be cleared and success navigation emitted exactly once, only after `POST /sales` completes successfully.
- REQ-SALE-SUBMISSION-INTEGRITY-003: A network, serialization, or server failure must preserve the cart and payment state, expose an actionable error, and allow retry without re-entering payments.
- REQ-SALE-SUBMISSION-INTEGRITY-004: While submission is in flight, repeated submit actions must not create additional backend requests or duplicate success/navigation events.
- REQ-SALE-SUBMISSION-INTEGRITY-005: Payment method, payment amount, discount, balance, and change must reject invalid states and use cent-safe arithmetic without binary floating-point loss in business calculations.
- REQ-SALE-SUBMISSION-INTEGRITY-006: Focused automated tests must cover exact, partial, and excess payments; invalid inputs; successful submission; failure preservation; retry; immutable payloads; and concurrent submit attempts.

## API/Data Contract

- Keep the existing `POST /sales` endpoint and request field names.
- Do not introduce a public backend contract change.
- The mobile boundary must never submit null payment methods or null/invalid payment amounts.
- Any internal money representation must map to the backend's existing numeric JSON representation at the DTO boundary.
- A successful call is the only authority for transitioning the mobile flow to completed.

## Mobile UX And Lifecycle

- Loading state: submission is visibly in progress and the final action is disabled or ignored.
- Empty state: submission is rejected when the cart or valid payments are absent.
- Error/retry state: a clear error is shown; cart and payments remain unchanged; retry is available.
- Permission behavior: unchanged; this feature does not modify camera permissions.
- Navigation/back behavior: success navigation occurs once after backend confirmation; failure does not navigate.
- Offline or slow network behavior: state remains submitting during the request; offline/timeout transitions to retryable error without losing data.

## Acceptance Criteria

- A successful submission sends the complete pre-reset payload and clears local state exactly once.
- A failed submission neither navigates nor clears cart/payment data.
- Retrying after failure submits one sale without duplicating payment entries.
- Rapid or concurrent submit actions create one backend request while the first request is active.
- Exact payment, multiple partial payments, and overpayment produce the correct balance and change to the cent.
- Null payment methods, blank/invalid/zero/negative amounts, `NaN`, infinite values, negative discounts, and discounts greater than the allowed total are rejected with observable feedback.
- Existing `POST /sales` field names and JSON number compatibility are preserved.

## Test Strategy

- Unit tests:
  - pure cent-safe sale calculation and validation;
  - state transitions for submitting, success, failure, and retry;
  - fake repository captures an immutable payload and request count;
  - regression tests for exact, partial, excess, and invalid payments.
- Instrumented/Compose tests:
  - not required for the first implementation slice unless observable UI feedback cannot be proven at the ViewModel/state boundary.
- Manual/emulator checks:
  - slow successful submission;
  - offline failure followed by retry;
  - rapid repeated taps on the final action.

## MCP Sources

- Local architecture, testability, and code-review agents.
- `app/src/main/java/com/m4/red_android/viewmodels/BarcodeViewModel.kt`
- `app/src/main/java/com/m4/red_android/viewmodels/SalesViewModel.kt`
- `app/src/main/java/com/m4/red_android/data/models/Sales.kt`
- `app/src/main/java/com/m4/red_android/ui/scanner/PaymentInputCard.kt`
- No external source was required.
