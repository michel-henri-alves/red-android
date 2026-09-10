# Sale ViewModel And Compose Verification Spec

## Problem

Sale domain/coordinator behavior has strong JVM coverage, but ViewModel adaptation,
Compose rendering, navigation effects, and presentation-level money handling lack
direct regression tests.

## Scope

- In scope: BarcodeViewModel tests, fake product/sales dependencies, Compose payment tests, navigation/effect tests, lifecycle collection, accessible loading/error UI, and removal of remaining presentation money ambiguity.
- Out of scope: new sale behavior, backend changes, Room outbox, camera refactoring.

## Impact Classification

- Impact: high
- Creates new domain/workflow: no
- Changes domain model: no
- Changes public API contract: no
- Changes durable architecture/project memory: yes
- Canonical docs required: app spec/tasks/memory

## Criticality

Critical verification work for payment/sales behavior.

## Requirements

- REQ-SALE-UI-TEST-001: ViewModel tests must prove reset/navigation only after success and state preservation on failure.
- REQ-SALE-UI-TEST-002: Compose tests must prove submitting, retryable error, retry, partial payment, and change-dialog behavior.
- REQ-SALE-UI-TEST-003: Completion/navigation effects must be consumed once across recomposition and lifecycle restart.
- REQ-SALE-UI-TEST-004: Presentation money must be formatted from cent-safe values without business arithmetic in composables.
- REQ-SALE-UI-TEST-005: Test fixtures must not depend on global Retrofit, real camera, wall clock, or dispatcher.

## API/Data Contract

No backend contract changes expected.

## Mobile UX And Lifecycle

- Verify loading/error/retry accessibility, button state, dialog dismissal, back navigation, and lifecycle-safe flow collection.

## Test Strategy

- Unit: ViewModel with injected fakes and clock.
- Instrumented/Compose: payment and navigation semantics.
- Device: exact/partial/excess/failure/retry visual checks.

## MCP Sources

- Sale feature 0001, current ViewModel/composables, Android testing guidance.
