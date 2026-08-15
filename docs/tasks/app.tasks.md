# RED Android App Tasks

- [ ] Keep app-level SDD docs aligned with durable architecture changes.
- [ ] Add focused tests as ViewModel, API, and UI workflows mature.
- [ ] Keep build, lint, unit test, and contract gates runnable from npm scripts.

## Sale Submission Integrity

- [x] REQ-SALE-SUBMISSION-INTEGRITY-001 — Create and submit an immutable, complete
  sale snapshot through a repository boundary.
- [x] REQ-SALE-SUBMISSION-INTEGRITY-002 — Clear state and emit completion only after
  confirmed backend success.
- [x] REQ-SALE-SUBMISSION-INTEGRITY-003 — Preserve state on failure and retry the same
  snapshot without duplicating payments.
- [x] REQ-SALE-SUBMISSION-INTEGRITY-004 — Enforce coordinator-level single-flight and
  disable final UI actions during submission.
- [x] REQ-SALE-SUBMISSION-INTEGRITY-005 — Use `Long` cents for production payment,
  discount, balance, and change calculations with observable invalid-input feedback.
- [x] REQ-SALE-SUBMISSION-INTEGRITY-006 — Cover calculations, payload copying,
  success, failure, retry, and concurrent submission with focused JVM tests.
- [ ] Complete the critical-feature release gates and manual slow/offline/rapid-tap
  evidence in feature `0001-sale-submission-integrity-and-characterization-tests`.
