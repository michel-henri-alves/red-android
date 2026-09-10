# Scanner Camera Lifecycle Hardening Spec

## Problem

Camera ownership, permission outcomes, barcode debounce, audio resources, and screen
lifecycle are coupled to the POS ViewModel/UI. This risks leaks, duplicate reads, blank
camera states, and poor behavior on devices without available camera hardware.

## Scope

- In scope: lifecycle-aware camera bind/unbind, permission state model, denial/settings UX, scanner event deduplication, ToneGenerator/resource release, camera-unavailable fallback, rotation/background tests, and scanner/POS separation seam.
- Out of scope: new barcode formats, product creation redesign, visual scanner redesign beyond states/accessibility.

## Impact Classification

- Impact: high
- Creates new domain/workflow: no
- Changes domain model: no
- Changes public API contract: no
- Changes durable architecture/project memory: yes
- Canonical docs required: app spec/tasks/memory

## Criticality

High mobile lifecycle and permission risk.

## Requirements

- REQ-CAMERA-001: Camera binding must follow the visible screen lifecycle and release resources when stopped/disposed.
- REQ-CAMERA-002: Permission states must distinguish requestable denial, permanent denial/settings, granted, and unavailable hardware.
- REQ-CAMERA-003: Duplicate detections within the configured policy must produce one product lookup while distinct scans remain responsive.
- REQ-CAMERA-004: Audio/scanner resources must be owned and released explicitly without ViewModel leaks.
- REQ-CAMERA-005: Scanner failures must expose recoverable UI and must not corrupt cart state.
- REQ-CAMERA-006: Unit, fake-camera, Compose, and device tests must cover lifecycle and permission transitions.

## API/Data Contract

No public backend change. Product lookup behavior remains aligned with the existing contract.

## Mobile UX And Lifecycle

- Loading: camera initialization indicator.
- Empty: scanning frame without cart items.
- Error/retry: camera unavailable/bind failure action.
- Permission: rationale, retry, settings action, and no-camera fallback.
- Navigation/back: unbind when leaving scanner and restore safely on return.
- Offline: product lookup error does not break camera lifecycle or cart.

## Test Strategy

- Unit: dedup policy/resource owner.
- Compose/fake camera: permissions and lifecycle rendering.
- Device: deny/permanent deny/grant, background/return, rapid scan, navigation.

## MCP Sources

- Current camera/scanner/permission sources and feature 0003 test infrastructure.
