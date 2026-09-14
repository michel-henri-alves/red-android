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

## 2026-08-15 - Authentication, Session, And Network Foundation

- `RedApplication` owns the sole `SessionManager`; session state, not a parallel UI
  Boolean, gates root content and protected navigation.
- Tokens are persisted as versioned AES-256-GCM ciphertext under `noBackupFilesDir`,
  with the key held by Android Keystore. Corruption/key invalidation fails closed.
- Login has a separate public client. Product/sales clients add exactly one Bearer
  header, consume matching sliding renewal, and expire matching `401` requests without
  automatic replay.
- Release networking is HTTPS-only with logging disabled. Debug API selection uses the
  `redApiBaseUrl` Gradle property and a narrow local cleartext allowlist.
- Network logs omit bodies/query strings and redact authorization/renewal headers.
- Backend-verified JWT claims remain the tenant boundary; Android does not author
  `companyId` for protected sale items.
- Login and protected UI have no shared back stack. Physical Android 14 checks proved
  encrypted relaunch, offline relaunch, logout, and back isolation.
- Backend logout is local-only until a revocation contract exists. The configured
  production API hostname must resolve and pass deployment smoke tests before public
  distribution; local/device validation used USB forwarding to the local backend.

## 2026-09-04 - Tenant-Aware Password Recovery

- Public login and recovery identify an account with normalized `companyId` plus email;
  protected tenant authority continues to come only from the verified access token.
- Recovery state is ephemeral and uses generic accepted/error feedback. Loading begins
  synchronously before launching the request so rapid taps cannot enqueue duplicates.
- `requiresInitialPasswordChange` is stored inside the encrypted session projection.
  Root navigation selects only the password-change UI while it is true, including after
  process recreation, and never composes the protected navigation graph underneath it.
- Successful replacement updates the stored session projection; backend credential
  versioning invalidates older tokens. Physical Android 14 Compose tests cover recovery,
  mandatory replacement and root-back isolation.


## 2026-09-07 — ADR-0001 verification paused

- Static review inspected recovery, session/root routing, token renewal and separate
  public/authenticated Retrofit clients. No Android implementation changes this turn.
- Backend contract and SDD checks passed. New unit/lint run is NOT a pass: default
  Java 8 was incompatible; system Java 17 lacked javac. The complete Android Studio
  JDK allowed progress through resources into `compileDebugKotlin`.
- Command: `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest lintDebug --offline`.
  Log: `/tmp/adr0001-android-tests.log`. On user-requested pause, the run was
  interrupted with Ctrl-C (exit 130); do not treat historical XML reports as new evidence.
- No connected tests or device-to-backend journey were executed this turn. A proposed
  thread dump was aborted by the user and yielded no diagnostic result.
- T008 remains open. Resume from workspace
  `docs/features/ECO-0001-password-recovery/PAUSE.md`.

## Password recovery follow-up — 2026-09-08

- Resumed ECO-0001. R1/R2 are resolved locally: conditional credential writes and
  durable asynchronous request acceptance with migration 0005 and a scheduled worker.
- Real MongoDB/Mailpit, Chromium and Android device journeys passed. Web mandatory
  layout now hides business navigation; axe/keyboard/360px/1280px checks passed.
- Evidence: local feature `runs/2026-09-08-follow-up.md` and ecosystem
  `docs/features/ECO-0001-password-recovery/review-2026-09-08.md`.
- Production SMTP/scheduler/capacity and manual screen-reader/TalkBack/Android
  large-font/landscape review remain. ECO-T007 is open; no release approval implied.
- No commits, PRs or deployment. Preserve unrelated workspace changes.

## ECO-0002 company selection lifecycle

RedApplication owns FileCompanyContextStore in noBackupFilesDir, separate from SecureTokenStore. Production AuthViewModel receives CompanyAccessApi and store; legacy constructor defaults exist for existing isolated session tests. Company selection persists after successful matching login, revalidates before signed-out calls and rejects stale results by selection version. Scoped protected ViewModelStore prevents cache/form reuse across tenant sessions.


## Definitive domain — 2026-09-11

ECO-T009 selects `tipo.click` (AWS-registered) and `<accessName>.tipo.click`. The API URL remains unchanged. The existing DNS zone is associated with CloudFront FREE/ACTIVE. The user accepted the small variable deployment/verification charges and authorized continuation while preserving the existing infrastructure and avoiding any new paid plan or fixed monthly resource. Android keeps the shared API URL and the durable company-context implementation; no Android release is tied to each store hostname.

## Pause checkpoint — 2026-09-11

The Android contract check and SDD check passed. Android runtime/lifecycle tests were not rerun for this domain checkpoint. Resume after backend resolver/mappings are available; then verify saved-context revalidation against tipo.click access names without changing token authority.

## ECO-0002 pausado — 2026-09-13

Usuário pediu pausa. Registro canônico de retomada: [RESUME.md](../../../docs/features/ECO-0002-company-access-login/RESUME.md). Mapeamento restrito a m4 e ramon-lopes, regra de 2–63 caracteres. Backend 477 testes, web 73 testes/build, banco 3 testes e Android unitários/assembleDebug passaram. Rollout externo não confirmado; reconciliar chamada interrompida antes de nova execução. Sem celular físico no momento.


## ECO-0002 publicado — 2026-09-14

Backend versão 3, mapeamentos m4/ramon-lopes, certificado, DNS e web publicados na infraestrutura existente CloudFront Free/Active. Testes reais de navegador e Android de seleção/troca de empresa passaram. Relatório canônico: [release-2026-09-14.md](../../../docs/features/ECO-0002-company-access-login/release-2026-09-14.md). Esse registro prevalece sobre a pausa de 2026-09-13. Plano completo da API depende de leitura IAM; código segue local sem commit/push. Teste adicional de recuperação no aparelho aguarda reconexão.


## ECO-0002 — teste de recuperação concluído e envio autorizado

Em 2026-09-14, a jornada de recuperação no Moto G35 passou após reconexão, com fixture local/Mailpit. APK de produção reinstalado. Usuário autorizou commit/push; branch feat/company-access-login. Evidência versionável: [verification-2026-09-14.md](../features/0008-company-access-login/runs/verification-2026-09-14.md). Este registro substitui a pendência de reconexão anterior.
