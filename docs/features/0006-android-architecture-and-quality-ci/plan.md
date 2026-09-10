# Android Architecture And Quality CI Plan

## Files

- Gradle/version catalog, application/composition root, feature/core package moves, test fixtures, npm scripts, CI workflows, quality docs.

## Canonical Documentation

- Update `docs/specs/app.spec.md`, `docs/tasks/app.tasks.md`,
  `docs/memory/project.memory.md`, SDD quality gates, and README/contributor guidance.

## Context Bundle

- SDD/build/mobile context, completed feature evidence, current dependency/package graph.

## Agents

- `android-architecture-engineer`, `implementation-engineer`, `test-engineer`, `code-reviewer`, `release-gate-reviewer`

## Skills

- `red-android-testing-quality`, `red-sdd-feature-closure`

## Implementation Sequence

1. Baseline dependencies, warnings, coverage, and CI behavior.
2. Introduce composition root/DI without package moves.
3. Establish feature/core boundaries incrementally.
4. Add distinct formatting/static/coverage gates.
5. Add CI reports/artifacts and enforce policy.
6. Run full regression and update durable docs.

## Tests

- Architecture/dependency tests plus full project and device smoke gates.
- Commands: `npm run sdd:check`, `npm run contracts:check`, `npm run test`,
  `npm run test:coverage`, `npm run lint`, `npm run static:analysis`, `npm run build`.

## Gate Checks

- SDD, contracts, formatting, Detekt, test/coverage threshold, lint, build, CI dry run.

## Risks

- Large mechanical diffs, DI startup errors, flaky CI, and coverage gaming.

## Definition Of Done

- No global Retrofit access from features, boundaries are documented/enforced, CI is green, and no new warning/debt baseline is introduced.
