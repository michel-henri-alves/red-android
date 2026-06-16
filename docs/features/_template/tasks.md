# Feature Tasks: FEATURE_TITLE

- [ ] T001 - REQ-FEATURE-001 Add or update focused verification before implementation.
  - Agent: `test-engineer`
  - Depends on: none
  - Verification: `npm run test`
- [ ] T002 - REQ-FEATURE-001 Implement the requirement.
  - Agent: `implementation-engineer`
  - Depends on: T001
  - Verification: focused verification from T001
- [ ] T003 - REQ-FEATURE-001 Run project gates.
  - Agent: `implementation-engineer`
  - Depends on: T002
  - Verification: `npm run sdd:check`, `npm run contracts:check`, `npm run test`, `npm run lint`, and `npm run build`
