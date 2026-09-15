# RED Android App Tasks

- [x] Keep app-level SDD docs aligned with durable architecture changes.
- [x] Add focused tests as ViewModel, API, and UI workflows mature.
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

## Authentication And Network Foundation

- [x] REQ-AUTH-NET-001 — Own explicit session states at application scope and restore
  before choosing login or protected content.
- [x] REQ-AUTH-NET-002 — Attach one Bearer credential and safely consume sliding token
  renewal on protected calls.
- [x] REQ-AUTH-NET-003 — Handle local expiry, matching-token `401`, logout, and relogin
  without retry or navigation loops.
- [x] REQ-AUTH-NET-004 — Protect tokens with Android Keystore AES-GCM/no-backup storage
  and redact network secrets.
- [x] REQ-AUTH-NET-005 — Enforce HTTPS/no logging in release and explicit local-only
  cleartext configuration in debug.
- [x] REQ-AUTH-NET-006 — Guard login, token, tenant, expiry, and redaction contracts with
  JVM, MockWebServer, Compose, device, and cross-project checks.

## Password Recovery

- [x] REQ-ANDROID-RECOVERY-001..004 — Provide tenant-aware recovery DTOs, state and
  accessible login UI with generic feedback, throttle/error mapping and duplicate suppression.
- [x] REQ-ANDROID-RECOVERY-005 — Keep temporary-password sessions at the root mandatory
  change destination across back navigation and process recreation.
- [x] REQ-ANDROID-RECOVERY-006 — Replace the stored session state only after a successful
  password change and retain safe failure/expiry handling.
- [x] Verify recovery and mandatory-change Compose behavior on an Android 14 physical device.

## ECO-0002 company access login

- [x] Implement local company access-name behavior and coordinated compatibility changes.
- [ ] Complete environment-specific rollout checks and close ECO-0002 after integrated evidence and production inputs are recorded.
- Evidence and exact local verification results: docs/features/0008-company-access-login/runs/implementation-2026-09-10.md.
