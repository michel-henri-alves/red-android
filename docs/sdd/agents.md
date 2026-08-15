# RED Android Agents

## Recommended Sequence

1. `sdd-spec-reviewer` - spec completeness and ambiguity.
2. `sdd-planner` - implementation sequence and verification plan.
3. `android-architecture-engineer` - architecture boundaries and technical design for high-impact work.
4. `kotlin-software-engineer` - modern Kotlin design, implementation, patterns, and maintainability.
5. `implementation-engineer` - scoped implementation.
6. `test-engineer` - focused automated tests.
7. `backend-contract-reviewer` - Retrofit/OpenAPI/backend contract work.
8. `mobile-ux-regression-reviewer` - user-facing Compose/mobile workflows.
9. `security-tenant-isolation-reviewer` - auth, tenant, secret, and sensitive data flows.
10. `cross-project-integrator` - backend/web/mobile coordination.
11. `code-reviewer` - correctness and maintainability.
12. `performance-cost-reviewer` - runtime, battery, network, build, and AI context cost.
13. `release-gate-reviewer` - final evidence review.

## Context Bundle Rule

Give each agent only:

- `docs/sdd/constitution.md`
- `docs/sdd/workflow.md`
- `docs/sdd/context-map.md`
- the active agent file
- relevant feature files
- the smallest necessary code files
- matching skills from `docs/sdd/skills.md`
