# Backend Contract Spec

## Purpose

Define how `red-android` consumes `red-backend` APIs.

## Rules

- Use Retrofit services and DTOs that match backend method, path, headers, query params, request bodies, response bodies, and error shapes.
- Include auth and tenant/company context for protected tenant-scoped endpoints.
- Treat API error mapping as user-facing behavior.
- Check `../red-backend/docs/contracts/openapi.json` when available.

## Authentication Contract

- `POST /users/login` is explicitly unauthenticated and accepts required string fields
  `companyId`, `email`, and `password`.
- Success returns `accessToken` plus `user.name`, `user.role`, and `user.companyId`.
  The JWT expiry schedules local session expiry; client decoding is not authorization
  or signature verification.
- Protected requests send exactly one `Authorization: Bearer <token>` header.
- Authenticated responses may return a renewed token in `X-Access-Token`.
- A protected `401` expires the matching local session without request replay.
- Tenant scope comes from the backend's verified token. Sale items and other protected
  Android DTOs must not send client-authored `companyId` as authorization context.
- Logout is local-only until the backend publishes a revocation/refresh-token contract.
- `POST /users/password-recovery` is unauthenticated, accepts required `companyId`
  and `email`, and returns generic HTTP 202 feedback.
- Login exposes `requiresInitialPasswordChange`; Android routes such sessions only
  to the authenticated password-change API until completion.

## Verification

- `npm run contracts:check`
- Unit tests for DTO mapping and API error handling when behavior changes.

## Recovery acceptance refinement — 2026-09-08

The recovery `202` acknowledges asynchronous processing and does not promise email
has already arrived. Request/response DTOs remain unchanged. A real device journey
with Retrofit, Compose, MongoDB and Mailpit verified email delivery, temporary login,
mandatory replacement and old-session rejection. See the feature run report for
local-only setup; manual TalkBack/large-font/landscape review remains pending.

## ECO-0002 company discovery

CompanyAccessApi uses the public Retrofit client: POST companies/resolve-access, body {accessName}, success {companyId,accessName,name}. DTO maps to a versioned local CompanyContext; schemaVersion is never required from the server. Error codes 400/404/429/503 follow backend OpenAPI. Login/recovery bodies and token authorization are unchanged; returned login companyId must match the selected company.


## Definitive domain — 2026-09-11

ECO-T009 selects `tipo.click` (AWS-registered) and `<accessName>.tipo.click`. The API URL remains unchanged. The existing DNS zone is now associated with CloudFront FREE/ACTIVE. Local web production builds use the new base domain; DNS/TLS/application activation remains pending. No additional charges beyond registration/renewal are authorized for this task. See the ECO-0002 domain-migration subtask and the infrastructure run `docs/features/0002-company-access-login/runs/domain-cost-2026-09-11.md` for the actual billing verification and remaining limits.
