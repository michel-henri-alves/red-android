# Refactor With SDD Context

Use this prompt for scoped refactoring that preserves behavior.

## Instructions

- Preserve public behavior and backend contracts.
- Improve readability, testability, lifecycle safety, or Compose state separation.
- Avoid mixing feature changes with refactoring unless the SDD plan explicitly requires it.
- Add or update tests when refactoring affects behavior-sensitive code.

## Output

Report the refactor goal, files changed, and verification commands run.
