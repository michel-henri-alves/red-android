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

### Sale Submission Integrity

- A sale crosses the asynchronous repository boundary only as an immutable snapshot
  containing copied items, payments, discount, change, vendor, local correlation code,
  submission id, and a realization timestamp with the `America/Sao_Paulo` offset.
- `SaleSubmissionCoordinator` is the sole owner of submission, terminal transitions,
  single-flight behavior, and retry. ViewModels and composables must not call `SalesApi`
  directly or launch a second submission workflow.
- Cart and payment state is cleared, and completion/navigation is emitted, exactly once
  and only after the repository confirms `POST /sales` success.
- Failure preserves the visible cart/payment state and the exact failed snapshot. Retry
  resubmits that snapshot without rebuilding or appending payment entries.
- Business calculations and validation use `Money` backed by `Long` cents. `Double` is
  permitted only at legacy catalog and JSON DTO boundaries through explicit conversion.
- The final sale action is unavailable while submission is active. Invalid payment or
  discount input produces observable feedback and never reaches the repository.

### Sales API Boundary

- Android sends `SalesRequest` with `items`, `paymentMethod`, `amountPaid`, `discount`,
  `change`, `vendor`, and `realizedAt` using the existing `POST /sales` field names.
- `amountPaid` is a required non-null numeric list paired by index with `paymentMethod`.
- Tenant identity is supplied by authenticated root-sale context; clients do not send
  `companyId` for each item. Local sale/item codes are not part of the backend request.

## Verification

- `npm run sdd:check`
- `npm run contracts:check`
- `npm run test`
- `npm run test:coverage`
- `npm run lint`
- `npm run static:analysis`
- `npm run build`
