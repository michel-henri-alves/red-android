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
