# RED Android Improvement Roadmap

## Objective

Evolve the Android app from a functional POS prototype into a secure, testable,
recoverable, and release-ready client without mixing unrelated risks in one change.

## Delivery Order

| Order | Feature | Outcome | Depends on |
| --- | --- | --- | --- |
| 1 | `0002-auth-session-and-network-foundation` (complete) | Authenticated, environment-aware, safely logged API access | none |
| 2 | `0003-sale-viewmodel-and-compose-verification` | ViewModel/UI regression coverage and presentation-safe money state | 0002 |
| 3 | `0004-durable-sale-outbox-and-idempotency` | Process-death recovery, offline queue, and backend exactly-once protection | 0002, 0003 |
| 4 | `0005-scanner-camera-lifecycle-hardening` | Predictable camera ownership, permissions, resources, and duplicate-scan behavior | 0003 |
| 5 | `0006-android-architecture-and-quality-ci` | Dependency injection, feature boundaries, zero-new-warning policy, and CI gates | 0002–0005 |

## Operating Rules

- Execute one `Txxx` task at a time and record evidence in each feature folder.
- Do not start 0004 until authentication/tenant behavior is stable.
- Prefer behavior-preserving seams and tests before structural refactoring.
- Public backend contract changes require coordinated Android/backend SDD evidence.
- Close every feature with `npm run sdd:run -- <feature-id>`.

## Global Success Measures

- Authenticated requests do not leak tokens or cross tenant boundaries.
- Critical sale behavior has unit, ViewModel, Compose, and device evidence.
- A pending sale survives process death and retries without duplication.
- Camera resources follow screen lifecycle and permission outcomes.
- CI rejects contract drift, test failure, lint errors, and new static-analysis debt.
