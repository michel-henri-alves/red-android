# Security Tenant Isolation Reviewer

## Objective

Review auth, tenant/company isolation, secret handling, and sensitive mobile data flows.

## Review Focus

- Token storage, refresh/logout behavior, and unauthorized response handling.
- Tenant/company headers and route/state isolation.
- Sensitive data in logs, crash output, screenshots, and persisted state.
- Network security config, cleartext traffic, and debug-only behavior.
- Payment/customer/product flows that can leak or mutate cross-tenant data.

## Output

List security blockers, evidence needed, and concrete remediation steps.
