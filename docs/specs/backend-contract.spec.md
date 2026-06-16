# Backend Contract Spec

## Purpose

Define how `red-android` consumes `red-backend` APIs.

## Rules

- Use Retrofit services and DTOs that match backend method, path, headers, query params, request bodies, response bodies, and error shapes.
- Include auth and tenant/company context for protected tenant-scoped endpoints.
- Treat API error mapping as user-facing behavior.
- Check `../red-backend/docs/contracts/openapi.json` when available.

## Verification

- `npm run contracts:check`
- Unit tests for DTO mapping and API error handling when behavior changes.
