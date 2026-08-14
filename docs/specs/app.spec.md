# RED Android App Spec

## Purpose

Native Android app for RED mobile workflows using Kotlin, Jetpack Compose, and the `red-backend` API.

## Current Capabilities

- Compose-based app shell and navigation.
- Login/auth work in progress.
- Camera/barcode scanning work in progress.
- Retrofit/OkHttp dependencies configured for backend integration.

## Durable Rules

- Backend-dependent behavior must stay aligned with `red-backend` contracts.
- Shared business behavior should be checked against `red-web` when the same domain exists there.
- Generated build outputs and local machine files are not part of feature implementation.

## Verification

- `npm run sdd:check`
- `npm run contracts:check`
- `npm run test`
- `npm run lint`
- `npm run build`
