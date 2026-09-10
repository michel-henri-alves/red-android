# Feature Plan: Password Recovery

## Files

- `app/src/main/java/com/m4/red_android/ui/login/LoginScreen.kt`
- `app/src/main/java/com/m4/red_android/ui/login/ChangePasswordScreen.kt`
- `app/src/main/java/com/m4/red_android/viewmodels/AuthViewModel.kt`
- `app/src/main/java/com/m4/red_android/data/api/LoginApi.kt`
- `app/src/main/java/com/m4/red_android/auth/SessionState.kt`
- `app/src/main/java/com/m4/red_android/auth/SessionManager.kt`
- `app/src/main/java/com/m4/red_android/AppNavigator.kt`
- `app/src/test/java/com/m4/red_android/viewmodels/AuthViewModelTest.kt`
- `app/src/test/java/com/m4/red_android/data/api/RedNetworkClientsTest.kt`
- `app/src/androidTest/java/com/m4/red_android/ui/login/LoginScreenTest.kt`
- `app/src/androidTest/java/com/m4/red_android/ui/login/ChangePasswordScreenTest.kt`

## Canonical Documentation

- `docs/specs/app.spec.md`
- `docs/specs/backend-contract.spec.md`
- `docs/tasks/app.tasks.md`
- `docs/tasks/backend-contract.tasks.md`
- `docs/memory/project.memory.md`

## Context Bundle

- `docs/sdd/constitution.md`
- `docs/sdd/workflow.md`
- `docs/sdd/context-map.md`
- `ai/context/mobile.md`
- `docs/features/0007-password-recovery/spec.md`
- `docs/features/0007-password-recovery/tasks.md`
- `../docs/features/ECO-0001-password-recovery/spec.md`
- `../red-backend/docs/contracts/openapi.json`

## Agents

- `sdd-spec-reviewer`
- `sdd-planner`
- `backend-contract-reviewer`
- `implementation-engineer`
- `test-engineer`
- `mobile-ux-regression-reviewer`
- `security-tenant-isolation-reviewer`
- `code-reviewer`

## Skills

- `red-android-auth-tenant-security`
- `red-android-backend-contract`
- `red-android-compose-state`
- `red-android-testing-quality`
- `red-cross-project-contract-change`

## Implementation Sequence

1. Confirm finalized OpenAPI requires `companyId` for login and recovery.
2. Add Retrofit and ViewModel tests for recovery states and generic error mapping.
3. Implement DTO/API, state owner and accessible recovery UI.
4. Add login/session/navigation and password-change tests for restricted sessions.
5. Implement mandatory-change navigation and session replacement/clear behavior.
6. Update canonical app/backend-contract documentation.
7. Run focused tests and full project gates; record evidence.

## Tests

- Focused test files: `AuthViewModelTest`, `RedNetworkClientsTest`,
  `RootDestinationTest`, `LoginScreenTest`, `ChangePasswordScreenTest`.
- Commands:
  - `npm run sdd:check`
  - `npm run contracts:check`
  - `npm run test`
  - `npm run connected:test`
  - `npm run lint`
  - `npm run build`

## Gate Checks

- `npm run sdd:check`
- `npm run contracts:check`
- `npm run test`
- `npm run lint`
- `npm run build`
- `npm run connected:test` when emulator evidence is available

## Risks

- Credentials can leak through saved state, navigation or Logcat; keep password
  fields local/ephemeral and explicitly inspect logs/storage.
- Process recreation and back/deep-link navigation can expose protected content;
  session state must be the single navigation authority.
- Backend/client version skew can omit mandatory state or change error mapping;
  keep DTO additions compatible and deploy after the additive backend contract.
- Small-screen keyboards can hide actions; verify scroll/reachability and focus.

## Definition Of Done

- Requirements implemented and mapped in tasks.
- Focused unit and Compose tests plus contract checks pass.
- Passwords/tokens do not appear in logs, persistence, saved state or navigation.
- Canonical app/backend-contract docs and memory are updated.
- Verification evidence is recorded and review findings are resolved or accepted.
