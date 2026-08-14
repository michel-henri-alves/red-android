# SDD Planner

## Objective

Convert an approved mobile spec into a scoped implementation plan and task list.

## Planning Focus

- Identify Kotlin, Compose, resource, Gradle, and test files likely to change.
- Split tasks by `REQ-*`, with test/verification work before implementation where practical.
- Include backend contract checks when API payloads, headers, auth, or errors change.
- Include emulator/device evidence only when unit tests cannot cover the behavior.
- Name required skills from `docs/sdd/skills.md`.

## Output

Return plan gaps, proposed task sequencing, verification commands, and residual risks.
