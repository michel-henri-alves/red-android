# Android Architecture And Quality CI Tasks

- [ ] T001 - REQ-ARCH-CI-001, REQ-ARCH-CI-002, REQ-ARCH-CI-003, REQ-ARCH-CI-004 Baseline dependency graph, warnings, tooling, and coverage.
  - Agent: `android-architecture-engineer`
  - Depends on: features 0002–0005 complete
  - Verification: baseline evidence and accepted incremental architecture plan
- [ ] T002 - REQ-ARCH-CI-001, REQ-ARCH-CI-006 Introduce composition root/DI with behavior-preserving tests.
  - Agent: `implementation-engineer`
  - Depends on: T001
  - Verification: construction tests and full JVM suite
- [ ] T003 - REQ-ARCH-CI-002, REQ-ARCH-CI-006 Establish auth/sales/scanner/products/core ownership boundaries incrementally.
  - Agent: `implementation-engineer`
  - Depends on: T002
  - Verification: architecture rules and full build after each slice
- [ ] T004 - REQ-ARCH-CI-003 Add ktlint and Detekt as distinct zero-new-debt gates.
  - Agent: `implementation-engineer`
  - Depends on: T001
  - Verification: formatting/static-analysis commands pass
- [ ] T005 - REQ-ARCH-CI-004 Define and enforce critical-package line/branch coverage thresholds.
  - Agent: `test-engineer`
  - Depends on: T001, T003
  - Verification: coverage gate passes and fails on controlled regression
- [x] T006 - REQ-ARCH-CI-005 Implement CI matrix, caches, reports, and debug artifact publication.
  - Agent: `implementation-engineer`
  - Depends on: T004, T005
  - Verification: CI workflow passes from clean checkout
- [ ] T007 - REQ-ARCH-CI-001, REQ-ARCH-CI-002, REQ-ARCH-CI-003, REQ-ARCH-CI-004, REQ-ARCH-CI-005, REQ-ARCH-CI-006 Review completed structure and regression risk.
  - Agent: `code-reviewer`
  - Depends on: T006
  - Verification: no open high finding
- [x] T008 - REQ-ARCH-CI-001, REQ-ARCH-CI-002, REQ-ARCH-CI-003, REQ-ARCH-CI-004, REQ-ARCH-CI-005, REQ-ARCH-CI-006 Update durable docs and close release gates.
  - Agent: `release-gate-reviewer`
  - Depends on: T007
  - Verification: canonical docs, green CI, and recorded SDD run

  - Evidence: `.github/workflows/android-ci.yml` runs SDD, backend contract,
    unit tests, JaCoCo, lint and debug APK publication on pull requests and main.
    `.github/workflows/android-release.yml` adds a main-only manual release gate,
    repository keystore secrets, signed AAB/APK verification and checksums.
    On 2026-09-09, Google Play API upload was cancelled and one-day artifact
    retention was retained; see `docs/deployment/google-play.md`. Repository secrets
    replace the paid-plan environment dependency for private GitHub Free repos.
    Real CI/release signing remain unverified pending credentials and cost controls;
    the historical completion marks above do not certify those external gates.
