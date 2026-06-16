---
name: red-android-compose-state
description: Guidance for RED Android Jetpack Compose screens, UI state, navigation, accessibility, loading/error/empty states, and Compose testing. Use when implementing or reviewing Android UI, composables, navigation, forms, or user-facing mobile workflows.
---

# RED Android Compose State

## Use When

- Creating or changing Compose screens, forms, dialogs, lists, or navigation.
- Reviewing UI state, text overflow, accessibility, or user-facing regressions.
- Adding Compose or instrumented tests.

## Workflow

1. Identify state owners: composable local state, ViewModel state, navigation args, and backend-derived state.
2. Model loading, success, empty, error, retry, and disabled states explicitly.
3. Hoist state when multiple composables need it; keep visual-only state local.
4. Keep side effects in `LaunchedEffect`, ViewModel methods, or lifecycle-aware APIs, not in composable bodies.
5. Verify small screens, dark theme, keyboard behavior, back behavior, and text overflow for user-facing changes.
6. Prefer focused tests for state transitions and critical UI flows.

## Checks

- No network calls directly from composable rendering.
- No unstable loops caused by state writes during composition.
- Buttons and touch targets remain reachable on small screens.
- Permission denial and retry flows are visible when relevant.
