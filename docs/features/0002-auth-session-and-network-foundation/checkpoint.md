# Continuation Checkpoint

Closed on 2026-08-15 after completing T009.

## Resume Point

- Feature work is complete. Continue with feature 0003 in the Android improvement roadmap.
- Do not approve public production distribution until the configured API hostname
  resolves and a production login smoke test passes.

## Accepted Decisions

- One application-scoped `SessionManager` owns session state.
- Session states are `Restoring`, `Unauthenticated`, `Authenticated`, and `Expired`.
- The backend issues a 20-minute JWT and renews it through `X-Access-Token` on
  authenticated responses.
- Tenant identity comes from the verified JWT; Android must not manufacture tenant
  authorization data.
- Logout is local-only because the backend has no refresh-token or revocation contract.
- A stale `401` may expire only the token that made its request.
- Refreshed-token writes are serialized and may not replace the current token with an
  expired or older token.
- Production token storage will use direct Android Keystore AES-256-GCM and no-backup
  storage in T005.

## Implemented Through T008

- `auth/SessionState.kt`: state, session identity, expiry reason, and token metadata.
- `auth/SecureTokenStore.kt`: suspendable read/write/clear persistence boundary.
- `auth/SessionManager.kt`: restore, authenticate, replace token, active-token `401`
  invalidation, local expiry, logout, transition serialization, and fail-closed restore.
- `auth/SessionManagerTest.kt` and `FakeSecureTokenStore.kt`: eight focused JVM tests.
- `data/api/SessionInterceptor.kt`: single Bearer credential, sliding-token capture,
  matching-token `401` expiry, and no request replay.
- `data/api/SafeHttpLoggingInterceptor.kt`: debug-selectable header logging with
  credential redaction and no request/response body exposure.
- `data/api/SessionInterceptorTest.kt`: five focused MockWebServer network tests.
- `auth/KeystoreSecureTokenStore.kt`: Android Keystore AES-256-GCM key ownership,
  authenticated versioned ciphertext in `noBackupFilesDir`, atomic replacement, and
  file/key clearing.
- `auth/EncryptedSessionFileStoreTest.kt`: six focused encrypted persistence tests.
- `RedApplication.kt`: application-scoped session owner and asynchronous restoration.
- Backup configuration: application backup disabled with explicit cloud and
  device-transfer exclusions.
- `data/api/RetrofitClient.kt`: validated build-variant environment, separate public
  and authenticated clients, safe logging configuration, and compatibility facade.
- `data/api/LoginApi.kt`: characterized login request and safe response models.
- `data/api/RedNetworkClientsTest.kt`: five focused environment/client tests.
- Network security resources: HTTPS-only base/release policy and explicit debug-only
  local cleartext hosts.
- `AuthenticatedApp.kt`: single root owner mapping session state to restoring, login,
  expired-login, or protected content without a shared auth/protected back stack.
- `AuthViewModel.kt`: real login/session/logout commands and actionable error mapping.
- `LoginScreen.kt`: credential fields, obscured password, loading and error states.
- `AppNavigator.kt`: protected logout action.
- `AuthViewModelTest.kt` and `RootDestinationTest.kt`: nine focused JVM lifecycle tests.
- `LoginScreenTest.kt`: two compiled instrumented Compose interaction/state tests.
- `device-evidence.md`: Android 14 evidence for login, encrypted relaunch, offline
  relaunch, logout, back isolation, restored connectivity, and on-device test results.
- Forced light Material theme and password/email-specific IME configuration added from
  physical-device UX findings.
- Backend OpenAPI/login documentation and Android cross-project contract checks were
  aligned during T002; see `plan.md` for the full characterization.

## Last Verified State

- Focused auth/root tests: 9 passed.
- Full Android JVM suite: 53 passed.
- Debug instrumented test APK assembly: passed.
- Release APK assembly: passed.
- `npm run contracts:check`: passed.
- `npm run sdd:check`: passed, with warnings only for existing generated directories.
- `git diff --check`: passed.
- Gradle must run with a modern JDK on this machine; the working command used:
  `env JAVA_HOME=/opt/idea-IC-243.23654.153/jbr ./gradlew testDebugUnitTest`.

## Working Tree Note

The workspace intentionally contains uncommitted work from feature 0001, the roadmap,
features 0002-0006, and backend contract alignment. Preserve unrelated changes and do
not reset or discard the working tree when resuming.

## Closure

- Final traceability and release decision: `evidence.md`.
- Physical Android 14 results: `device-evidence.md`.
- Recorded passing SDD run: `runs/2026-08-15T20-54-05-964Z.md`.
