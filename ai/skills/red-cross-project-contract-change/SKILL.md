---
name: red-cross-project-contract-change
description: Coordinate RED Android, red-backend, and red-web changes when a feature changes shared business rules, API contracts, auth/tenant behavior, POS/payment/inventory behavior, or canonical SDD documentation.
---

# RED Cross Project Contract Change

## Use When

- Android behavior depends on backend or web changes.
- Backend API payloads, validation, statuses, or error semantics change.
- A shared domain rule must remain consistent across RED projects.

## Workflow

1. Name all affected projects: `red-android`, `red-backend`, `red-web`, and any database migration.
2. Identify rollout order and backward-compatibility requirements.
3. Update feature SDD docs in the project being changed and canonical docs where the durable rule lives.
4. Check backend OpenAPI and mobile DTOs together.
5. Verify equivalent user behavior in web/mobile for shared workflows.

## Checks

- `red-backend/docs/contracts/openapi.json` reviewed for API changes.
- Android Retrofit DTOs and request headers align with backend.
- `red-web` impact is explicitly accepted or planned.
