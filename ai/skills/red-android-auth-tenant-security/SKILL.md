---
name: red-android-auth-tenant-security
description: Guidance for RED Android authentication, token handling, tenant/company isolation, secure logging, unauthorized responses, network security, and sensitive mobile data. Use when changing login, session state, API authorization, tenant context, or security-sensitive flows.
---

# RED Android Auth Tenant Security

## Use When

- Implementing login, logout, token storage, tenant/company selection, protected routes, or authorized API calls.
- Reviewing logs, debug behavior, network config, or sensitive customer/payment data handling.

## Workflow

1. Identify where credentials, tokens, tenant/company ids, and user roles enter and leave the app.
2. Keep auth headers centralized and avoid duplicating token logic across services.
3. Handle 401/403 responses consistently with session state and user feedback.
4. Avoid logging tokens, passwords, raw payment/customer payloads, or full authorization headers.
5. Keep debug-only network/logging behavior from leaking into release behavior.
6. Add tests for session state transitions and tenant-specific request construction.

## Checks

- No secrets in staged files or committed config.
- Unauthorized responses do not leave stale protected UI active.
- Tenant/company context cannot be accidentally omitted for tenant-scoped backend calls.
