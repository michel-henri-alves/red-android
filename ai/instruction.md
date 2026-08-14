# AI Instructions for RED Android

## Project Context

`red-android` is the native Android mobile app for the RED commerce platform. It is written primarily in Kotlin and uses Jetpack Compose, Navigation Compose, CameraX, ML Kit barcode scanning, Retrofit/OkHttp, and the `red-backend` API.

## Development Guidelines

- Prefer small, testable Kotlin functions and focused composables.
- Keep UI state explicit and hoisted when possible.
- Keep network DTOs, domain state, and UI models separate when the feature grows beyond a trivial screen.
- Use coroutine-aware APIs and avoid blocking the main thread.
- Treat camera, storage, notification, and network permissions as user-facing flows with denial/retry states.
- Keep auth tokens and sensitive data out of logs, screenshots, and committed files.
- Do not change generated build outputs under `app/build`, `.gradle`, `app/debug`, or `app/release`.

## Architecture Pointers

- App entry point: `app/src/main/java/com/m4/red_android/MainActivity.kt`
- Navigation: `app/src/main/java/com/m4/red_android/AppNavigator.kt`
- Screens/composables: `app/src/main/java/com/m4/red_android/...`
- Unit tests: `app/src/test/java/...`
- Instrumented tests: `app/src/androidTest/java/...`
- Android resources: `app/src/main/res/...`

## Quality Expectations

- Add focused unit tests for ViewModel, mapping, validation, and API boundary logic.
- Add instrumented or Compose UI tests for critical mobile flows when feasible.
- Run `npm run test`, `npm run lint`, and `npm run build` before closing behavior-changing work.
- Use `npm run connected:test` only when an emulator/device is intentionally part of the verification evidence.
