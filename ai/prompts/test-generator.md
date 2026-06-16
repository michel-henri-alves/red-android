# Test Generator

Use the supplied SDD feature files and mobile context to add focused automated tests.

## Instructions

- Prefer unit tests for ViewModels, pure mapping/validation, state reducers, and API boundary logic.
- Add Compose or instrumented tests for critical user flows only when the project test setup supports it.
- Use stable test data and avoid real network calls.
- Cover loading, success, error, empty, retry, permission denied, and lifecycle-sensitive states when relevant.
- Keep tests close to the changed behavior and avoid broad snapshot-style assertions.

## Output

Report test files created or changed and the exact test command run.
