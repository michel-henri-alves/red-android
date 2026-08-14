# Scanner Spec

## Purpose

Camera and barcode scanning workflows support RED mobile product lookup and related POS/inventory flows.

## Rules

- Camera permission denial, retry, and success states must be visible.
- CameraX binding must be lifecycle-aware.
- Barcode detection must avoid repeated navigation or repeated backend calls for the same scan.
- Scanner UI must provide feedback for scanning, detected code, lookup progress, failure, and retry.

## Verification

- Unit tests for scanner state/debounce logic when present.
- Instrumented or manual emulator/device evidence for camera behavior.
