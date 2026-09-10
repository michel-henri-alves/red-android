# Auth Session And Network Foundation Spec

## Problem

The app calls authenticated backend endpoints without a complete session owner,
authorization interceptor, expiry handling, environment selection, or production-safe
logging. Sales cannot be considered operationally safe until tenant-authenticated
requests behave deterministically.

## Scope

- In scope:
  - model login/session states and token storage;
  - attach bearer credentials through an OkHttp interceptor;
  - handle `401`, expiry, logout, and relogin without retry loops;
  - separate development, staging, and production base URLs;
  - configure connect/read/write/call timeouts;
  - restrict body logging to debug builds and redact secrets;
  - verify tenant/auth headers against `red-backend`.
- Out of scope:
  - social login or biometrics;
  - backend authorization-policy redesign;
  - offline sale persistence;
  - broad feature modularization.

## Impact Classification

- Impact: high
- Creates new domain/workflow: yes
- Changes domain model: yes
- Changes public API contract: no
- Changes durable architecture/project memory: yes
- Canonical docs required: `docs/specs/app.spec.md`, `docs/tasks/app.tasks.md`, `docs/memory/project.memory.md`

## Criticality

Critical. Authentication and tenant isolation protect every backend-dependent workflow.

## Requirements

- REQ-AUTH-NET-001: One lifecycle-independent session owner must expose loading, authenticated, expired, and unauthenticated states.
- REQ-AUTH-NET-002: Authenticated requests must attach the current bearer token exactly once and never log credentials.
- REQ-AUTH-NET-003: `401` or local expiry must transition to an actionable signed-out state without infinite retry or navigation loops.
- REQ-AUTH-NET-004: Tokens must use Android-protected storage and be cleared on explicit logout or invalidation.
- REQ-AUTH-NET-005: Base URL, timeouts, and HTTP logging must be build-environment aware and production safe.
- REQ-AUTH-NET-006: Contract tests must prove login parsing, header construction, tenant behavior, expiry, and redaction.

## API/Data Contract

- Reuse existing backend login and authenticated endpoint contracts.
- Tenant identity remains token-derived; the Android client must not manufacture `companyId`.
- Any discovered backend drift requires `backend-contract-reviewer` approval before implementation.

## Mobile UX And Lifecycle

- Loading state: restore persisted session before choosing the first destination.
- Empty state: unauthenticated users see login.
- Error/retry state: distinguish invalid credentials, connectivity, expiry, and server failure.
- Permission behavior: unchanged.
- Navigation/back behavior: expired sessions return once to login and cannot back-navigate into protected screens.
- Offline or slow network behavior: preserve the session locally while exposing connectivity failure; do not treat every timeout as logout.

## Test Strategy

- Unit tests: session reducer/store, interceptor, authenticator loop prevention, expiry, environment config.
- Instrumented/Compose tests: login states, protected navigation, logout.
- Manual/device checks: login, relaunch restore, expiry, offline launch, logout.

## MCP Sources

- `../red-backend/routes`, auth middleware, OpenAPI, and local Android network/session sources.
