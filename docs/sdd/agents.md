# RED Android Agents

## Recommended Sequence

1. `sdd-spec-reviewer` - spec completeness and ambiguity.
2. `sdd-planner` - implementation sequence and verification plan.
3. `implementation-engineer` - scoped implementation.
4. `test-engineer` - focused automated tests.
5. `backend-contract-reviewer` - Retrofit/OpenAPI/backend contract work.
6. `mobile-ux-regression-reviewer` - user-facing Compose/mobile workflows.
7. `security-tenant-isolation-reviewer` - auth, tenant, secret, and sensitive data flows.
8. `cross-project-integrator` - backend/web/mobile coordination.
9. `code-reviewer` - correctness and maintainability.
10. `performance-cost-reviewer` - runtime, battery, network, build, and AI context cost.
11. `release-gate-reviewer` - final evidence review.

## Context Bundle Rule

Give each agent only:

- `docs/sdd/constitution.md`
- `docs/sdd/workflow.md`
- `docs/sdd/context-map.md`
- the active agent file
- relevant feature files
- the smallest necessary code files
- matching skills from `docs/sdd/skills.md`
