# AI-Assisted Development for RED Android

This directory contains prompts, agents, skills, context, and adapters for SDD-driven AI development in `red-android`.

## Structure

- `instruction.md` - General instructions for AI assistants.
- `context/mobile.md` - Android/Kotlin/Compose project context.
- `prompts/` - Action-level prompt templates.
- `agents/` - Role-specific SDD agents.
- `skills/` - Project-specific capability guides loaded only when relevant.
- `adapters/` - CLI wrappers for Codex, Claude, Copilot, and compact execution logs.

## Usage

Create SDD feature files:

```bash
npm run sdd:new
npm run sdd:new:ai
```

List agents:

```bash
npm run sdd:agents
```

Generate an agent prompt for a feature:

```bash
npm run sdd:agent -- 0001-feature-slug spec-reviewer
npm run sdd:agent -- 0001-feature-slug planner
```

Run verification and record evidence:

```bash
npm run sdd:run -- 0001-feature-slug
```

Install local hooks:

```bash
npm run hooks:install
```

The default mobile gate is:

```bash
npm run sdd:check
npm run contracts:check
npm run test
npm run lint
npm run build
```
