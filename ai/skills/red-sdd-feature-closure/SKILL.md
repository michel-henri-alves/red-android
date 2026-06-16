---
name: red-sdd-feature-closure
description: Close RED Android SDD features with complete spec, plan, tasks, canonical docs, verification evidence, run logs, and release gate review. Use before marking a feature complete or merging high-impact work.
---

# RED SDD Feature Closure

## Use When

- A feature is ready for final review.
- High-impact Android work changed domain behavior, backend contracts, security, permissions, or reusable architecture.

## Workflow

1. Ensure `spec.md`, `plan.md`, and `tasks.md` are present and free of clarification markers.
2. Confirm every `REQ-*` has implementation and verification tasks.
3. Confirm high-impact work references canonical docs under `docs/specs`, `docs/tasks`, or `docs/memory`.
4. Run and record verification with `npm run sdd:run -- {feature}`.
5. Use `release-gate-reviewer` for final evidence review when release risk matters.

## Checks

- No incomplete placeholders.
- Verification evidence is current.
- Required generated or release artifacts were not accidentally changed.
