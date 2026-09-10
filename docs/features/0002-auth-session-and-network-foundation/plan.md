# Auth Session And Network Foundation Plan

## Files

- `app/src/main/java/com/m4/red_android/data/api/RetrofitClient.kt`
- `app/src/main/java/com/m4/red_android/viewmodels/AuthViewModel.kt`
- `app/src/main/java/com/m4/red_android/AppNavigator.kt`
- new `auth/` and `core/network/` production/test files
- Gradle build configuration and manifest network policy as required

## Canonical Documentation

- Update app spec/tasks/memory with session ownership, token storage, and network policy.

## Context Bundle

- SDD constitution/workflow/context map, `ai/context/mobile.md`, backend auth routes/middleware/OpenAPI, and this feature package.

## Agents

- `security-tenant-isolation-reviewer`, `backend-contract-reviewer`, `implementation-engineer`, `test-engineer`, `mobile-ux-regression-reviewer`, `release-gate-reviewer`

## Skills

- `red-android-backend-contract`, `red-android-testing-quality`, `red-sdd-feature-closure`

## Architecture And Security Decision

Status: accepted on 2026-08-15 for T001.

### Current-State Findings

- `AuthViewModel.login()` ignores credentials and sets `isLoggedIn = true`; the login
  screen uses fixed placeholder credentials and immediately navigates on button press.
- `AppNavigator` starts directly in the POS and has no protected-route/session gate.
- `RetrofitClient` is a global singleton with no credential source, interceptor,
  timeout policy, response-token handling, or build-aware logging.
- The backend issues a 20-minute bearer JWT containing `userId`, `companyId`, and
  `role`, and returns a refreshed token in `X-Access-Token` after authenticated calls.
- App backup/device-transfer rules do not exclude future credential storage, while
  `android:allowBackup` is enabled.
- The current network security config permits cleartext for a local development host;
  this must never be inherited by release configuration.

### Session Ownership

- One application-scoped `SessionManager` is the authoritative session owner. It is
  created by the application composition root, not by a composable or ViewModel, and
  exposes a read-only `StateFlow<SessionState>`.
- The state model is `Restoring`, `Unauthenticated`, `Authenticated(session)`, and
  `Expired`. `AuthenticatedSession` contains the bearer token plus minimal display
  identity returned by login. Tenant/role claims are informational on the client;
  backend verification remains authoritative.
- `AuthViewModel` submits login/logout commands to the session owner and maps session
  state to UI. It does not persist tokens or construct HTTP headers.
- Navigation derives protected access from `SessionState`; it does not maintain a
  second Boolean. Restore completes before selecting login or protected navigation.
- Product and sale repositories receive the authenticated network client through the
  composition root. No feature reads credential storage directly.

### Protected Token Storage

- Define a narrow `SecureTokenStore` interface so storage behavior is independently
  testable and replaceable.
- Production storage encrypts the token with AES-256-GCM using a non-exportable key
  generated directly in the `AndroidKeyStore`. Store a fresh random IV with each
  ciphertext and never reuse an IV/key pair.
- Store encrypted material under `noBackupFilesDir`; also add explicit cloud-backup
  and device-transfer exclusions as defense in depth. Do not store the bearer token in
  plain SharedPreferences, DataStore, saved state, Bundle, database, logs, or analytics.
- Keystore invalidation, authentication-tag failure, corrupt/truncated ciphertext, or
  incompatible restore must delete the unreadable record and transition to
  `Unauthenticated`; the app must never loop or crash during restore.
- Do not adopt deprecated `EncryptedSharedPreferences`, `MasterKey`, or other
  `androidx.security:security-crypto` APIs. Android's current guidance favors platform
  cryptography and direct Android Keystore use.

### Token Lifecycle And Concurrency

- Login success persists the token before publishing `Authenticated`; persistence
  failure is an authentication failure and must not expose protected navigation.
- An application interceptor adds exactly one `Authorization: Bearer <token>` header
  to protected requests. The login call uses an explicitly unauthenticated client/path.
- A network response interceptor observes `X-Access-Token`. Concurrent refreshed-token
  writes are serialized; T002 must confirm claim semantics, after which the store keeps
  only a token that is not older than the current token's expiry.
- Local JWT decoding may be used only to schedule expiry and compare refresh freshness;
  it is not signature verification and must not grant role or tenant authority.
- A `401` invalidates the matching active token once and transitions to `Expired`.
  There is no automatic replay/authenticator loop until a refresh-token contract exists.
- Explicit logout clears memory and protected storage before publishing
  `Unauthenticated`. Server-side revocation is not assumed because no revocation
  contract has been confirmed.

### Threat Boundaries And Controls

| Threat | Control / decision |
| --- | --- |
| Token disclosure through logs | Redact `Authorization`, `X-Access-Token`, password, and login bodies; body logging debug-only |
| Token restored onto another device | `noBackupFilesDir` plus explicit backup/device-transfer exclusions |
| Tenant spoofing | Never send client-authored `companyId`; backend derives tenant from verified JWT |
| Retry/redirect header duplication | Replace/remove then add one authorization header; test redirects and retries |
| Concurrent refresh rollback | Serialize writes and retain the freshest confirmed token |
| Expired-token loops | One-way `401` invalidation; no automatic request replay |
| Corrupt/invalidated keystore entry | Clear token and fail closed to unauthenticated state |
| MITM/cleartext release traffic | HTTPS-only release config; local cleartext limited to explicit debug config |
| Process memory inspection/rooted device | Token lifetime/minimal copies reduce exposure; compromised/rooted OS is residual risk |
| Screenshot/clipboard credential leakage | Password field is obscured; credentials are never copied or persisted |

### Alternatives Rejected

- A ViewModel-owned session was rejected because navigation, workers, and repositories
  outlive or exist outside one screen ViewModel.
- Plain SharedPreferences/DataStore token storage was rejected because app-private
  storage alone does not protect bearer-token contents at rest.
- `EncryptedSharedPreferences` was rejected because the AndroidX API is deprecated.
- Refreshing/replaying inside an OkHttp `Authenticator` was deferred because the
  backend currently exposes sliding access-token renewal, not a refresh-token contract;
  implicit replay could duplicate mutation requests.
- Trusting decoded `companyId`/`role` client-side was rejected as an authorization
  boundary; only the backend can verify JWT integrity.

### Follow-up Questions Routed To T002

- Confirm login request/response error shapes and whether `X-Access-Token` is returned
  consistently for every authenticated endpoint/proxy deployment.
- Confirm JWT clock-skew expectations, renewal behavior, and whether logout/revocation
  exists or must remain local-only.
- Confirm whether any endpoint intentionally accepts client tenant headers/fields.

### Official Android Security Sources

- Android Keystore: `https://developer.android.com/privacy-and-security/keystore`
- Cryptography guidance: `https://developer.android.com/privacy-and-security/cryptography`
- Auto Backup exclusions: `https://developer.android.com/identity/data/autobackup`
- AndroidX security deprecations: `https://developer.android.com/jetpack/androidx/releases/security`

## Backend Contract Characterization

Status: accepted on 2026-08-15 for T002.

### Login Contract

| Concern | Executable contract |
| --- | --- |
| Endpoint | `POST /users/login`; explicitly unauthenticated |
| Request | JSON object with required string fields `email` and `password` |
| Success | `200` with `accessToken` and `user`; `user` contains `name`, `role`, and `companyId` |
| Missing credentials | `400` with `error: "Email and password are required"` |
| Unknown email | `401` with `error: "Invalid email or password"` |
| Wrong password | `401` with `message: "Invalid credentials"` |

- The Android adapter must normalize the backend's heterogeneous `error`/`message`
  failure bodies into one UI error model and must not expose credential-specific
  distinctions to the user.
- The OpenAPI `LoginResponse` now describes the safe user subset actually returned by
  the controller instead of referring to the complete user entity.

### Token, Expiry, And Renewal

- The access token is a signed JWT with `userId`, `companyId`, `role`, `iat`, and `exp`.
- Runtime configuration and executable tests define a 20-minute lifetime (`1200`
  seconds). Stale backend feature documentation that said 10 minutes was aligned to
  this executable contract during T002.
- Every route protected by the shared authentication middleware returns a newly signed
  access token in `X-Access-Token`; CORS exposes this header to clients. The new token
  starts a fresh 20-minute window, implementing sliding renewal.
- Renewal is opportunistic: clients must remain correct when the response header is
  absent because of an endpoint/proxy/configuration fault. A successful response
  without it does not invalidate the current token.
- There is no refresh token, token-revocation endpoint, logout endpoint, or server-side
  blacklist. Logout therefore clears the local session only; a stolen token remains
  valid until its signed expiry.
- The client may decode `exp` without trusting the token to schedule expiry and choose
  the freshest concurrently returned token. A `401` remains the authoritative signal
  that the active session can no longer access protected resources.

### Tenant And Authorization Boundary

- The backend derives tenant identity from the verified JWT `companyId` claim and
  writes that value into tenant-scoped requests. Android must not originate or override
  `companyId` as an authorization mechanism.
- `role` and `companyId` returned by login may support display and navigation hints,
  but only backend verification authorizes an operation.
- A legacy tenant-header middleware exists in the repository but is not mounted by the
  active application routes and is not part of the mobile contract.
- The current authentication middleware extracts the second authorization-header
  segment without strictly rejecting schemes other than `Bearer`. Android will always
  send the standard `Bearer` scheme; strict server-side scheme validation is a backend
  hardening follow-up, not a client compatibility dependency.

### T002 Outcome

- No contract decision blocks T003 or T004. Tests may fix the 20-minute expiry,
  replacement-token header, local-only logout, token-derived tenant, and normalized
  login-error behavior as accepted assumptions.
- `red-android/scripts/check-backend-contract.js` now guards the unauthenticated login
  operation, required login fields, and the minimal successful response shape in
  addition to existing cross-project checks.
- Residual risks are the absence of server revocation and strict Bearer-scheme parsing;
  neither can be solved by trusting more client-provided data or replaying mutations.

## Test-First Session Behavior

Status: implemented and verified on 2026-08-15 for T003.

- `SessionManager` owns a read-only state flow starting in `Restoring`, with explicit
  `Unauthenticated`, `Authenticated`, and actionable `Expired` states.
- The narrow suspendable `SecureTokenStore` boundary supports read, write, and clear;
  its Android Keystore implementation remains deliberately scoped to T005.
- Restore accepts only a non-expired persisted session. Missing storage becomes
  unauthenticated; expired storage is cleared and reported as locally expired; an
  unreadable/corrupt record is cleared best-effort and fails closed to unauthenticated.
- Authentication persists before publishing protected state. A persistence failure
  leaves the previous signed-out state unchanged.
- Sliding-token replacement is serialized, requires the expected active token, and
  rejects expired or older replacements so concurrent responses cannot roll back the
  session.
- A `401` invalidates only the token that made the request, preventing a late response
  from expiring a newer session. Local expiry and explicit logout clear persistence;
  logout publishes unauthenticated even if cleanup reports a failure.
- Eight focused JVM tests cover these invariants. The complete 28-test JVM suite,
  backend contract check, SDD check, and diff whitespace check pass.

## Implementation Sequence

1. Review auth/tenant contract and decide session/token ownership.
2. Add session/interceptor tests and protected token storage.
3. Introduce environment-aware client configuration and safe logging.
4. Integrate login/expiry/logout navigation.
5. Run security, contract, UI, device, and release gates.

## Tests

- Focused test files:
  - `app/src/test/java/com/m4/red_android/auth/SessionStoreTest.kt`
  - `app/src/test/java/com/m4/red_android/core/network/AuthInterceptorTest.kt`
  - `app/src/test/java/com/m4/red_android/viewmodels/AuthViewModelTest.kt`
  - `app/src/androidTest/java/com/m4/red_android/auth/AuthNavigationTest.kt`
- Commands: `npm run sdd:check`, `npm run contracts:check`, `npm run test`,
  `npm run connected:test`, `npm run lint`, `npm run static:analysis`, `npm run build`.

## Gate Checks

- `npm run sdd:check`, `npm run contracts:check`, `npm run test`, `npm run connected:test`, `npm run lint`, `npm run static:analysis`, `npm run build`

## Risks

- Retry loops, token leakage, incorrect expiry interpretation, and tenant spoofing.
- Residual risk accepted for T001: a rooted/compromised device can observe process
  memory; server revocation and refresh-token rotation require contract decisions in T002.

## Definition Of Done

- Requirements covered, security review has no high finding, device flows pass, canonical docs and SDD run are recorded.
