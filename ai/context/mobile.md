# RED Android Mobile Context

## Product Role

The mobile app is part of the RED multi-tenant commerce platform and consumes the `red-backend` API. It should stay contract-compatible with backend endpoints and aligned with `red-web` for shared business behavior.

## Current Technical Shape

- Kotlin Android application.
- Jetpack Compose UI with Material 3.
- Navigation Compose for screen flow.
- CameraX and ML Kit for barcode scanning.
- Retrofit, Gson/Moshi converters, and OkHttp logging interceptor for HTTP integration.
- Gradle Kotlin DSL.

## Mobile-Specific Constraints

- Design for intermittent connectivity, slow startup, lifecycle restarts, rotation, permission denial, and background/foreground transitions.
- Keep camera resources lifecycle-aware and release them when screens leave composition.
- Avoid logging tokens, credentials, raw payment/customer data, and full request bodies.
- Prefer explicit loading, empty, error, and retry UI states for backend calls.
- Validate backend contract changes against `../red-backend/docs/contracts/openapi.json` when available.

## Verification Commands

- `npm run sdd:check`
- `npm run contracts:check`
- `npm run test`
- `npm run lint`
- `npm run build`
- `npm run connected:test` when emulator/device coverage is required.
