# Feature Plan: FEATURE_TITLE

## Files

- `path/to/file.kt`
- `path/to/test.kt`

## Canonical Documentation

- No canonical docs required for low-impact work.

## Context Bundle

- `docs/sdd/constitution.md`
- `docs/sdd/workflow.md`
- `docs/sdd/context-map.md`
- `ai/context/mobile.md`
- `docs/features/FEATURE_ID/spec.md`
- `docs/features/FEATURE_ID/tasks.md`

## Agents

- `sdd-spec-reviewer`
- `sdd-planner`
- `implementation-engineer`
- `test-engineer`
- Add `backend-contract-reviewer`, `mobile-ux-regression-reviewer`, `security-tenant-isolation-reviewer`, or `release-gate-reviewer` when relevant.

## Skills

- Add matching skills from `docs/sdd/skills.md`.

## Implementation Sequence

1. Add or update focused verification.
2. Implement the smallest behavior slice.
3. Run focused tests.
4. Run project gates.

## Tests

- Focused test files:
- Commands:
  - `npm run sdd:check`
  - `npm run contracts:check`
  - `npm run test`
  - `npm run lint`
  - `npm run build`

## Gate Checks

- `npm run sdd:check`
- `npm run contracts:check`
- `npm run test`
- `npm run lint`
- `npm run build`

## Risks

- Document lifecycle, permission, backend, or release risks.

## Definition Of Done

- Requirements implemented.
- Tests and verification evidence recorded.
- Canonical docs updated if impact classification requires them.
