# Physical Device Evidence

Recorded on 2026-08-15 for T008.

## Environment

- Device: Motorola moto g35 5G (`ZF525GFC55`)
- OS: Android 14
- App: debug variant
- Backend: local `http://localhost:3001/` through USB `adb reverse tcp:3001 tcp:3001`
- Original device connectivity: Wi-Fi enabled; mobile data enabled

## Results

| Check | Result | Evidence |
| --- | --- | --- |
| Cold signed-out launch | Pass | Login rendered without protected content or premature camera prompt; password semantics were protected. |
| Invalid credentials | Pass | Reachable local backend returned `401`; UI displayed the invalid-credentials action. |
| Connectivity failure | Pass | Unresolvable production endpoint produced the dedicated connectivity message without authenticating. |
| Valid login | Pass | Backend logged a successful login and the device rendered protected `Vendas` content with `Sair`. |
| Process relaunch | Pass | Force-stop/start restored encrypted session directly to protected content without login. |
| Offline relaunch | Pass | With Wi-Fi and mobile data disabled, force-stop/start restored protected content locally. |
| Connectivity restoration | Pass | Wi-Fi and mobile data were restored to their original enabled values after the offline check. |
| Logout | Pass | `Sair` cleared protected content and rendered login. |
| Back/reopen isolation | Pass | Back from login returned to launcher; reopening stayed on login with no protected content. |
| Instrumented tests | Pass | Three tests completed on the physical device through `connectedDebugAndroidTest`. |

## Findings Corrected During The Run

- System dark mode reduced login readability. The app now supplies an explicit light
  Material 3 scheme and light Android window theme in both day and night resources.
- The obscured password field still used generic text IME behavior. It now uses
  `KeyboardType.Password` with autocorrect and capitalization disabled; the email field
  uses the corresponding email keyboard configuration.

## Residual Scope

- The production API Gateway hostname did not resolve during this run, so successful
  lifecycle checks used the already-running local backend over USB forwarding.
- Natural 20-minute expiry and sliding renewal were not delayed for in this physical
  run; their matching-token behavior remains covered by deterministic JVM/MockWebServer tests.
