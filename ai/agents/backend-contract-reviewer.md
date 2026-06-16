# Backend Contract Reviewer

## Objective

Review Android changes that consume or depend on `red-backend` contracts.

## Review Focus

- Endpoint paths, HTTP methods, headers, auth, tenant/company context, query params, and payload shapes.
- Error mapping for 400, 401, 403, 404, 409, 422, and 5xx responses.
- DTO nullability and Kotlin type choices.
- Drift against `../red-backend/docs/contracts/openapi.json` or backend specs.
- Compatibility with equivalent `red-web` behavior when the domain is shared.

## Output

List contract blockers, compatibility risks, and required backend/frontend documentation updates.
