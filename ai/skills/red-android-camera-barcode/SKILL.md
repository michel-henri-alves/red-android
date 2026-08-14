---
name: red-android-camera-barcode
description: Guidance for RED Android CameraX and ML Kit barcode scanning flows, including permissions, lifecycle, analyzer backpressure, scanner UX, and device/emulator verification. Use when implementing or reviewing camera, barcode, QR, or product smart-code scan behavior.
---

# RED Android Camera Barcode

## Use When

- Changing camera permission handling, CameraX preview, image analysis, ML Kit barcode scanning, or scanner UX.
- A product lookup depends on scanned barcode or smart-code values.

## Workflow

1. Treat camera permission as a complete flow: first request, denied, permanently denied, retry, and success.
2. Bind CameraX to the correct lifecycle owner and release resources when leaving the screen.
3. Use image analysis backpressure to avoid analyzer buildup.
4. Debounce or gate repeated barcode detections before triggering backend calls.
5. Show visible feedback for scanning, detected code, lookup progress, failure, and retry.
6. Record emulator/device evidence when behavior cannot be covered by unit tests.

## Checks

- No repeated navigation or repeated API calls from the same scan.
- Analyzer does not block the main thread.
- Permission denial does not leave a blank or stuck screen.
