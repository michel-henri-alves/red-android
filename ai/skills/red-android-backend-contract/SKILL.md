---
name: red-android-backend-contract
description: Guidance for RED Android integration with red-backend APIs, Retrofit DTOs, auth/tenant headers, OpenAPI drift, error mapping, and cross-project contract changes. Use when Android code calls backend endpoints or changes request/response behavior.
---

# RED Android Backend Contract

## Use When

- Adding or changing Retrofit services, DTOs, auth headers, filters, pagination, or error handling.
- A feature depends on `red-backend` behavior or OpenAPI documentation.
- Android and `red-web` must stay behavior-compatible for a shared domain.

## Workflow

1. Read the relevant Android API code and the feature SDD files.
2. Compare with `../red-backend/docs/contracts/openapi.json` when available.
3. Confirm method, path, headers, query params, request body, response body, and error responses.
4. Map nullable fields deliberately; do not hide backend nullability with unsafe defaults.
5. Keep tenant/company/auth context explicit and tested.
6. Update canonical docs for high-impact contract changes.

## Checks

- `npm run contracts:check`
- Focused unit tests for DTO mapping/error handling when behavior changes.
- Cross-project plan if backend changes are required.
