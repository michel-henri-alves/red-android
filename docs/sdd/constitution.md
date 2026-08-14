# RED Android SDD Constitution

## Principles

1. Specs precede non-trivial implementation.
2. Every feature requirement uses a stable `REQ-*` id.
3. Mobile behavior includes lifecycle, permissions, loading, error, retry, accessibility, and offline implications when relevant.
4. Backend contract assumptions are explicit and checked against `red-backend` when available.
5. High-impact work updates canonical docs before closure.
6. Verification evidence is recorded for completed SDD work.

## High-Impact Mobile Work

Treat work as high impact when it:

- creates a new app workflow or domain area;
- changes backend API contracts or DTO semantics;
- changes auth, tenant/company isolation, permissions, payment, sales, inventory, or customer data;
- changes camera/scanner behavior;
- creates reusable architecture patterns;
- changes durable project memory.

## Forbidden Shortcuts

- Do not implement features with unresolved `[NEEDS CLARIFICATION: ...]` markers.
- Do not commit secrets, local properties, build artifacts, `.gradle`, `app/debug`, or `app/release`.
- Do not use live backend calls in automated tests.
