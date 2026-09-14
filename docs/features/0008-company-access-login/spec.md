---
ecosystem_feature: ECO-0002
ecosystem_requirements:
  - REQ-ECO-002
  - REQ-ECO-004
  - REQ-ECO-005
  - REQ-ECO-006
  - REQ-ECO-007
  - REQ-ECO-010
  - REQ-ECO-011
project: red-android
local_feature: docs/features/0008-company-access-login
---

# Company Access Login — red-android

**Status:** implemented locally — release verification pending

## Problem

Typing internal companyId makes login unnecessarily difficult. This project implements its part of [ECO-0002](../../../../docs/features/ECO-0002-company-access-login/spec.md).

## Scope

Introduce one-time access-name setup and durable company context independent of SessionManager; keep existing authentication and outbox lifecycle protections.

## Impact Classification

- Impact: high
- Creates new domain/workflow: yes
- Changes domain model: no
- Changes public API contract: yes
- Changes durable architecture/project memory: yes
- Impacted canonical docs: docs/specs/app.spec.md, docs/specs/backend-contract.spec.md, docs/tasks/app.tasks.md, docs/tasks/backend-contract.tasks.md, docs/memory/project.memory.md

## Criticality

Critical — authentication and tenant context.

## Requirements

- REQ-ECO-002: Provide public exact-match resolution of an access name to only `companyId`, `accessName` and display `name` for an enabled company. No company list, autocomplete, fuzzy search, user lookup or business data is exposed. Apply bounded input validation and distributed rate limiting.
- REQ-ECO-004: On Android first access, ask for the company access name, resolve it, show the company name and allow confirmation/correction before email/password login. Persist the selected company only after a successful login with matching response companyId; a mandatory-password-change session counts as authenticated.
- REQ-ECO-005: Store confirmed Android company context durably and separately from session secrets. Restore it after process death, restart, logout and session expiry. Reinstallation or app-data clearing requires setup again; exclude this preference from Android backup/restore. Do not persist credentials in this preference.
- REQ-ECO-006: Provide explicit company switching from signed-out Android login. Clear form credentials and recovery messages; cancel/ignore stale requests and retain the previous confirmed preference until a new successful login. Switching must never mix tokens, caches or pending tenant-owned operations. Authenticated users must log out first; retain existing outbox logout protections.
- REQ-ECO-007: Continue sending resolved companyId with email/password to existing login and with email to recovery. Preserve generic account errors, temporary-password and mandatory-change behavior. Authenticated authorization derives only from verified token claims; resolution and locally stored context grant no authorization.
- REQ-ECO-010: Cover loading, invalid input, unknown/unavailable company, throttling, offline/network failure and retry. Saved app context may render offline, but login/recovery need the backend. Revalidate saved accessName before a new login/recovery attempt; a changed companyId blocks submission and requires explicit setup.
- REQ-ECO-011: Record local and integrated verification for two companies, same email across companies, lifecycle persistence, switching races, password recovery, old-client compatibility, migration reruns and tenant hostname isolation before rollout.

## API/Data Contract

Use the exact resolver request, response, errors and normalization in the linked ecosystem specification. Keep existing login/recovery companyId payloads. No token is issued by resolution. Trusted provisioning assigns stable Company.accessName; public resolution is not authorization.

## UI States / Mobile UX And Lifecycle

Initial/loading, invalid input, company confirmation, unavailable company, throttled, offline/retry and authenticated/mandatory-change. Clear obsolete errors on edits, label controls accessibly, disable duplicate submissions and ignore stale async responses. No new permission required. Back from setup returns to prior confirmed company when available. Never persist password form state.

## Data Impact

Optional Company.accessName during migration with a unique partial index; internal IDs unchanged. Android company preference is separate from token/session data. Do not drop or rewrite existing records on rollback.

## Test Strategy

Validate the behavior slices listed in plan.md, including error paths, compatibility and two-company isolation. Runtime checks are execution tasks, not evidence claimed by this scaffold.

## MCP Sources

Source of truth: user-approved product direction, ECO-0002 specification, local source files listed in plan.md. No external service or production data queried during planning.

## Acceptance Criteria

All assigned requirements have local evidence; integration expectations in ECO-0002 pass for this project's producer/consumer responsibilities.

## Out Of Scope

Public directory, fuzzy company-name matching, QR/deep links, custom customer domains, SSO, simultaneous tenant sessions and user-facing access-name rename.


## ECO-T009 — tipo.click (2026-09-11)

The definitive application base domain is `tipo.click`, registered in AWS. Web entry is `https://<accessName>.tipo.click`; the apex gives company-access guidance. The API endpoint remains `https://7700ezljb5.execute-api.us-east-1.amazonaws.com`, including Android. Domain rollout must add no charges beyond registration/renewal. The existing Route53 zone has been associated with the active CloudFront Free plan; publication costs, certificate, reviewed company mappings and integrated acceptance remain pending. See the [domain migration subtask](../../../../docs/features/ECO-0002-company-access-login/domain-migration.md).
