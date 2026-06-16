# SDD Operation Manual for RED Android

## Create A Feature

```bash
npm run sdd:new
```

The wizard creates:

```text
docs/features/NNNN-feature-slug/spec.md
docs/features/NNNN-feature-slug/plan.md
docs/features/NNNN-feature-slug/tasks.md
```

## Review

```bash
npm run sdd:agents
npm run sdd:agent -- NNNN-feature-slug spec-reviewer
npm run sdd:agent -- NNNN-feature-slug planner
```

## Implement With AI

Use `ai/adapters/codex.sh`, `ai/adapters/claude.sh`, or `ai/adapters/copilot.sh` with the feature id and action:

```bash
ai/adapters/codex.sh NNNN-feature-slug implement
ai/adapters/codex.sh NNNN-feature-slug test
ai/adapters/codex.sh NNNN-feature-slug refactor
```

## Verify

```bash
npm run sdd:run -- NNNN-feature-slug
```

Default commands:

- `npm run sdd:check`
- `npm run contracts:check`
- `npm run test`
- `npm run lint`
- `npm run build`

Use `npm run connected:test` when emulator/device evidence is required.
