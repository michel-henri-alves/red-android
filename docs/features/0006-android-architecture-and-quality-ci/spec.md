# Android Architecture And Quality CI Spec

## Problem

Global clients, broad ViewModels/packages, duplicated lint/static-analysis commands,
remaining warnings, and locally executed gates make changes harder to isolate and
quality regressions easier to merge.

## Scope

- In scope: dependency injection/composition root, feature/core package boundaries, environment configuration consolidation, Detekt/ktlint, zero-new-warning policy, meaningful coverage rules for critical packages, CI workflow, cache/artifact policy, and contributor documentation.
- Out of scope: rewrite into many Gradle modules immediately, UI redesign, unrelated domain changes.

## Impact Classification

- Impact: high
- Creates new domain/workflow: no
- Changes domain model: no
- Changes public API contract: no
- Changes durable architecture/project memory: yes
- Canonical docs required: app spec/tasks/memory and contributor/quality docs

## Criticality

High release-engineering and maintainability impact; changes should be behavior preserving.

## Requirements

- REQ-ARCH-CI-001: Production dependencies must be supplied through a documented composition root rather than global service access.
- REQ-ARCH-CI-002: Sales, scanner, products, auth, and shared network/testing code must have explicit ownership boundaries without circular dependencies.
- REQ-ARCH-CI-003: Formatting and static analysis must run as distinct deterministic gates with a zero-new-debt policy.
- REQ-ARCH-CI-004: Critical packages must have enforced coverage thresholds based on meaningful line/branch baselines.
- REQ-ARCH-CI-005: CI must run SDD, contract, tests, coverage, lint, static analysis, and build using JDK 17 and publish useful reports/artifacts.
- REQ-ARCH-CI-006: Refactoring must preserve behavior and keep every intermediate task buildable.

## API/Data Contract

No public contract change. Contract checks remain mandatory during moves/refactors.

## Mobile UX And Lifecycle

Behavior-preserving; no intentional UX/navigation/permission change.

## Test Strategy

- Architecture rules, DI construction, static-tool fixtures, CI validation, full regression and smoke build.

## MCP Sources

- Completed features 0001–0005, build/quality scripts, package graph, canonical docs.
