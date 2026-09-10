---
ecosystem_feature: ECO-0001
ecosystem_requirements:
  - REQ-ECO-001
  - REQ-ECO-005
  - REQ-ECO-006
  - REQ-ECO-008
project: red-android
local_feature: docs/features/0007-password-recovery
---

# Feature Spec: Password Recovery

## Problem

The Android login screen has no recovery entry point. The app must initiate the
same privacy-preserving backend flow as the web client and safely constrain a
temporary-password session until the user chooses a personal password.

## Scope

- In scope: Compose recovery action/form, Retrofit contract, ViewModel states,
  generic feedback, retry/throttle handling, mandatory password-change navigation,
  session updates, accessibility and focused unit/Compose tests.
- Out of scope: backend credential generation/enforcement, SMS/link recovery,
  administrator reset and changing the account email.

## Impact Classification

- Impact: high
- Creates new domain/workflow: yes
- Changes domain model: no
- Changes public API contract: yes
- Changes durable architecture/project memory: yes
- Canonical docs required:
  - `docs/specs/app.spec.md`
  - `docs/specs/backend-contract.spec.md`
  - `docs/tasks/app.tasks.md`
  - `docs/tasks/backend-contract.tasks.md`
  - `docs/memory/project.memory.md`

## Criticality

Critical: authentication, security, tenant identity and backend-contract sensitive.

## Requirements

- REQ-ANDROID-RECOVERY-001: The signed-out Compose login screen exposes an
  accessible recovery action/form requiring normalized `companyId` and email.
- REQ-ANDROID-RECOVERY-002: Recovery state explicitly models idle, validation,
  loading, generic accepted, throttled, connectivity/server failure and retry;
  duplicate submissions are disabled while loading.
- REQ-ANDROID-RECOVERY-003: Accepted feedback never confirms account existence,
  status or delivery, and no password/authorization value is logged or placed in
  saved state, navigation arguments or persistent recovery state.
- REQ-ANDROID-RECOVERY-004: Retrofit DTOs and error mapping match the approved
  backend method, route, body, `202`, `400`, generic `429`/`Retry-After` and errors.
- REQ-ANDROID-RECOVERY-005: Login maps the backend mandatory-change flag into
  session/navigation state; a temporary-password session can reach only the
  password-change screen and back navigation cannot expose authenticated content.
- REQ-ANDROID-RECOVERY-006: Successful mandatory change replaces session data as
  defined by the backend, clears credential form state and enters normal app
  navigation; invalid/expired sessions return to login with safe feedback.

## API/Data Contract

- `POST users/password-recovery`, unauthenticated, required request
  `{ companyId, email }`.
- Valid syntax: `202` with `password.recovery.request.accepted`; invalid syntax:
  `400`; throttle: generic `429` and optional `Retry-After`.
- Login request becomes `{ companyId, email, password }`; its response adds the
  approved mandatory-change boolean to `LoginUser`.
- Authenticated password change retains the current three password fields during
  the compatibility window. Tenant scope for it comes only from the verified token.

## Mobile UX And Lifecycle

- Loading state: visible progress, disabled inputs/submission and no duplicate call.
- Empty state: no account-derived content; preserve only non-sensitive identity
  input across ordinary recomposition if consistent with the login pattern.
- Error/retry state: inline validation and generic connectivity/server/throttle copy.
- Permission behavior: no device permission is required.
- Navigation/back behavior: recovery can return to login; mandatory change cannot
  back-navigate into protected content; success clears the restricted destination.
- Offline or slow network behavior: retained non-secret input, progress, safe retry
  and no claim that mail was delivered before a `202` response.

## Test Strategy

- Unit tests: DTO serialization, ViewModel transitions, `202`/`400`/`429`, IO/server
  failures, duplicate suppression, login mandatory flag and successful session update.
- Instrumented/Compose tests: discovery, accessible labels/tags, validation,
  loading, generic accepted/error copy, small-screen keyboard reachability and back behavior.
- Manual/emulator checks: slow/offline network, rotation/recreation policy, dark
  theme, no credentials in Logcat/storage/navigation and restricted deep links.

## MCP Sources

- Local Android login, auth/session and Retrofit files; linked backend OpenAPI and
  `ECO-0001`. No external MCP source used.

## Acceptance Criteria

- A signed-out user can request recovery and safely return to login.
- All accepted states use non-enumerating generic copy.
- A temporary-login session never renders ordinary authenticated destinations.
- Backend contract check, focused unit/Compose tests and all Android gates pass.
