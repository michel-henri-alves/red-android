# SDD Feature Execution Prompt

Implement the requested SDD action using the supplied feature documents.

Rules:
- Treat `docs/sdd/constitution.md` as the highest-priority project contract.
- If an `ai/agents/{agent}.md` file is supplied, follow it as the role-specific execution contract.
- Use requirement ids from the feature spec and tasks to keep work traceable.
- If no specific `Txxx` task is supplied, execute all incomplete tasks in `tasks.md` in dependency order.
- If a specific `Txxx` task is supplied, implement only that task and its declared dependencies.
- Do not mark unrelated tasks complete during a task-focused run.
- Read only the Android screens, ViewModels, API clients, DTOs, repositories, navigation, tests, and docs needed for the files listed in the plan.
- Prefer existing Android, Kotlin, Compose, coroutine, ViewModel, and Retrofit patterns over new abstractions.
- Preserve auth, tenant context, navigation behavior, lifecycle safety, and backend API contracts.
- Include loading, error, empty, and success states when applicable.
- Run `npm run sdd:check`, `npm run contracts:check`, `npm run test`, `npm run lint`, and `npm run build`.
- Update feature tasks when work is completed.
- Keep each completed task tied to its `REQ-*`, agent, dependency, and verification metadata.
- If code and spec disagree, stop expanding scope and update the spec or plan explicitly.

Output budget:
- Keep the final response minimal: at most 20 lines.
- On success, return only a compact summary of changed files and verification status.
- Do not paste full file contents, full diffs, or full command output.
- Do not include command logs for successful commands.
- For failed commands, include only the relevant error lines and the next corrective action.
- Put long verification evidence in `docs/features/<feature-id>/runs/` instead of the chat/stdout response.
- Never print full diffs, full test output, dependency trees, or repeated file listings unless explicitly requested.
