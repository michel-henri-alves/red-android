# RED Android Project Memory

## 2026-06-16 - SDD Structure

`red-android` uses an SDD structure aligned with `red-backend` and `red-web`, adapted for Kotlin, Jetpack Compose, Android lifecycle, permissions, CameraX/ML Kit scanning, Retrofit backend contracts, mobile security, and emulator/device verification.

## 2026-08-15 - Sale Submission Integrity

- Sale completion is backend-confirmed: local cart/payment reset and navigation must
  never precede a successful repository response.
- `SaleSubmissionCoordinator` is the authoritative submission state machine. It uses
  local single-flight protection and retains the immutable failed snapshot for retry.
- Sale snapshots copy their item/payment collections before suspension and retain a
  stable submission id, vendor, local code, realization timestamp, discount, and change.
- Financial business logic uses `Money(Long cents)` and `SaleCalculator`; floating-point
  values exist only at validated legacy catalog and API serialization boundaries.
- The Android `SalesRequest` mirrors the backend wire fields. Payment methods and
  amounts are paired, non-null lists; item tenancy comes from the authenticated sale.
- End-to-end exactly-once behavior across ambiguous timeouts or process death still
  requires a future backend idempotency key. Current protection is client-process local.
