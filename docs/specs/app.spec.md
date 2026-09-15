# RED Android App Spec

## Purpose

Native Android app for RED mobile workflows using Kotlin, Jetpack Compose, and the `red-backend` API.

## Current Capabilities

- Compose-based app shell and navigation.
- Tenant-aware login, password recovery, mandatory password replacement, protected
  navigation, encrypted session restore, expiry, and logout.
- Camera/barcode scanning work in progress.
- Retrofit/OkHttp dependencies configured for backend integration.

## Durable Rules

- Backend-dependent behavior must stay aligned with `red-backend` contracts.
- Shared business behavior should be checked against `red-web` when the same domain exists there.
- Generated build outputs and local machine files are not part of feature implementation.

### Authentication And Session

- One application-scoped `SessionManager` is the authority for `Restoring`,
  `Unauthenticated`, `Authenticated`, and `Expired` state. UI and navigation derive
  from that state; a ViewModel or composable must not own a second auth Boolean.
- The access token is encrypted with an Android Keystore AES-256-GCM key and stored as
  authenticated, versioned ciphertext under `noBackupFilesDir`. Corruption or key
  invalidation fails closed and clears the session.
- Login uses a separate unauthenticated Retrofit client. Product and sales APIs use an
  authenticated client that replaces any prior authorization header with exactly one
  current Bearer token.
- `401` expires only the token that made the request and never automatically replays it.
  `X-Access-Token` sliding renewal replaces only the matching session with a non-older,
  non-expired token.
- Login and protected content do not share a navigation back stack. Restore completes
  before either is selected; logout and expiry cannot navigate back into protected UI.
- Logout is local because the backend currently exposes no refresh-token revocation
  contract. Connectivity failures do not clear a locally valid session.
- Login and recovery require normalized `companyId` plus email. Recovery feedback is
  generic and recovery form values are never persisted.
- A login response with `requiresInitialPasswordChange=true` selects the root password
  replacement screen before the protected `NavHost` is composed. This restriction is
  persisted with the encrypted session, survives process recreation, and consumes root
  back navigation; the application currently registers no external deep links.
- Successful password replacement persists the unrestricted session projection. A
  rejected or expired replacement leaves the session restricted or expires it through
  the normal authenticated-client `401` handling.

### Network And Tenant Security

- Release builds use the configured HTTPS production API, reject cleartext, and disable
  network logging. Debug may opt into `redApiBaseUrl`; cleartext remains limited by the
  debug network-security allowlist.
- Logs never read bodies, omit query strings, and redact `Authorization` and
  `X-Access-Token` headers.
- Tenant identity is derived by the backend from the verified JWT. Android may retain
  returned company/role values for display hints but must not manufacture tenant
  authorization fields in protected requests.
- App backup and device transfer are disabled; session ciphertext also resides in the
  platform no-backup directory.

### Sale Submission Integrity

- A sale crosses the asynchronous repository boundary only as an immutable snapshot
  containing copied items, payments, discount, change, vendor, local correlation code,
  submission id, and a realization timestamp with the `America/Sao_Paulo` offset.
- `SaleSubmissionCoordinator` is the sole owner of submission, terminal transitions,
  single-flight behavior, and retry. ViewModels and composables must not call `SalesApi`
  directly or launch a second submission workflow.
- Cart and payment state is cleared, and completion/navigation is emitted, exactly once
  and only after the repository confirms `POST /sales` success.
- Failure preserves the visible cart/payment state and the exact failed snapshot. Retry
  resubmits that snapshot without rebuilding or appending payment entries.
- Business calculations and validation use `Money` backed by `Long` cents. `Double` is
  permitted only at legacy catalog and JSON DTO boundaries through explicit conversion.
- The final sale action is unavailable while submission is active. Invalid payment or
  discount input produces observable feedback and never reaches the repository.

### Sales API Boundary

- Android sends `SalesRequest` with `items`, `paymentMethod`, `amountPaid`, `discount`,
  `change`, `vendor`, and `realizedAt` using the existing `POST /sales` field names.
- `amountPaid` is a required non-null numeric list paired by index with `paymentMethod`.
- Tenant identity is supplied by authenticated root-sale context; clients do not send
  `companyId` for each item. Local sale/item codes are not part of the backend request.

## Verification

- `npm run sdd:check`
- `npm run contracts:check`
- `npm run test`
- `npm run test:coverage`
- `npm run lint`
- `npm run static:analysis`
- `npm run build`

## ECO-0002 company setup

First signed-out access asks for a company access name (e.g. minha-loja), then shows “Entrar em <company name>” with a correction/switch action before email/password submission. FileCompanyContextStore atomically persists companyId/accessName/name/schemaVersion only after matching authenticated login, including mandatory-password-change sessions. Stored context survives logout, process recreation and restart; noBackupFilesDir plus existing backup exclusions prevent OS restore after reinstall.

Before login/recovery, AuthViewModel revalidates the saved name and ID. Mismatch requires setup; unavailable company offers correction/retry. Switching preserves the previous durable choice until successful replacement and invalidates late requests. Protected ViewModels have a company/session-scoped store that is cleared on leaving protected navigation, preventing previous-tenant form/cache reuse.

No durable sale outbox exists in current source; the separate planned outbox feature is not implemented here. This change introduces no deletion of durable sales records. Existing sessions without accessName remain usable until sign-out, then setup is required.
