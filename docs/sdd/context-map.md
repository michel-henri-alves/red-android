# RED Android Context Map

## Core Project Files

- `build.gradle.kts` - root Gradle configuration.
- `app/build.gradle.kts` - Android app module, dependencies, Compose, test setup.
- `gradle/libs.versions.toml` - version catalog.
- `app/src/main/AndroidManifest.xml` - app manifest and permissions.
- `app/src/main/java/com/m4/red_android/MainActivity.kt` - entry point.
- `app/src/main/java/com/m4/red_android/AppNavigator.kt` - navigation.
- `app/src/main/java/com/m4/red_android/CameraPreviewWithBarcodeReader.kt` - camera/barcode flow.
- `app/src/main/java/com/m4/red_android/ui/...` - UI feature areas.
- `app/src/main/java/com/m4/red_android/viewmodels/...` - ViewModel state.

## Test Locations

- `app/src/test/java/...` - local JVM unit tests.
- `app/src/androidTest/java/...` - instrumented/Compose tests.

## Adjacent Projects

- `../red-backend` - API, contracts, business rules, auth/tenant behavior.
- `../red-web` - web UX for shared RED workflows.

## SDD Context

- `ai/context/mobile.md` - mobile architecture and verification context.
- `ai/agents/` - role prompts.
- `ai/skills/` - conditional project guidance.
- `docs/features/` - feature-specific SDD packages.
- `docs/specs/` and `docs/tasks/` - canonical domain docs.
- `docs/memory/project.memory.md` - durable decisions.
