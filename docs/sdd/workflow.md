# RED Android SDD Workflow

## 1. Specify

Create `docs/features/NNNN-feature-slug/spec.md` with problem, scope, impact classification, requirements, API/data contract, tests, and MCP/source notes.

## 2. Plan

Create `plan.md` with affected files, context bundle, agents, skills, implementation sequence, tests, gate checks, risks, and Definition of Done.

## 3. Task

Create `tasks.md` with `Txxx` tasks mapped to `REQ-*` ids. Each task must include Agent, Depends on, and Verification metadata.

## 4. Review

Use:

```bash
npm run sdd:agent -- NNNN-feature-slug spec-reviewer
npm run sdd:agent -- NNNN-feature-slug planner
```

## 5. Implement

Implement one focused task at a time. Prefer tests before behavior changes when practical.

## 6. Verify

Run relevant commands and record evidence:

```bash
npm run sdd:run -- NNNN-feature-slug
```

## 7. Close

Update canonical docs for high-impact work, complete tasks, and run release gate review when needed.
