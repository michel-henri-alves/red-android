# Durable Sale Outbox And Idempotency Spec

## Problem

Local single-flight protects rapid actions only while one process is alive. A sale can
still be lost after process death or duplicated after an ambiguous timeout because no
durable outbox or backend idempotency key exists.

## Scope

- In scope: Room-backed immutable sale outbox, explicit pending/sending/confirmed/failed states, WorkManager retry, network constraints/backoff, process-death recovery, user-visible pending sales, backend idempotency key, reconciliation, and migration/retention policy.
- Out of scope: generic synchronization platform, inventory offline cache, conflict editing of already submitted snapshots.

## Impact Classification

- Impact: high
- Creates new domain/workflow: yes
- Changes domain model: yes
- Changes public API contract: yes
- Changes durable architecture/project memory: yes
- Canonical docs required: Android and backend sale specs/tasks/memory plus OpenAPI

## Criticality

Critical financial integrity and durable-data change.

## Requirements

- REQ-SALE-OUTBOX-001: A validated snapshot must be committed locally before network submission and survive process/device restart.
- REQ-SALE-OUTBOX-002: One stable idempotency key must identify the same logical sale across retries and be enforced atomically by the backend.
- REQ-SALE-OUTBOX-003: WorkManager must retry eligible failures with constraints/backoff without parallel submission of one key.
- REQ-SALE-OUTBOX-004: The UI must distinguish pending, sending, confirmed, and actionable failed sales without falsely reporting completion.
- REQ-SALE-OUTBOX-005: Ambiguous responses must reconcile by idempotency key before creating another sale.
- REQ-SALE-OUTBOX-006: Schema migration, retention, encryption/sensitive-data review, and tenant isolation must be tested.

## API/Data Contract

- Add an idempotency key to `POST /sales` using an agreed header or request field.
- Duplicate keys for the same tenant return the original committed result; cross-tenant reuse must not disclose data.
- Update backend validation, unique index, service transaction semantics, OpenAPI, and Android contract checks together.

## Mobile UX And Lifecycle

- Loading: show durable pending/sending state.
- Empty: no pending sales.
- Error/retry: actionable permanent failures; automatic retry for transient failures.
- Navigation: local queueing is not equivalent to backend-confirmed completion.
- Offline: accept only explicitly supported queued workflow and show its status honestly.

## Test Strategy

- Unit: outbox transitions, serialization, backoff/reconciliation.
- Database/worker/integration: migration, restart, unique work, process death.
- Backend: atomic duplicate-key and tenant tests.
- Device: offline submit, kill/relaunch, reconnect, ambiguous timeout.

## MCP Sources

- Feature 0001 evidence, feature 0002 auth/session, Room/WorkManager local docs, backend sale contract/storage.
