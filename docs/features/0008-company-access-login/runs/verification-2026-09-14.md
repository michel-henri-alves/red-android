# Company access login — verification on 2026-09-14

The release is active at https://m4.tipo.click and https://ramon-lopes.tipo.click; https://tipo.click displays company-address guidance. Only the two approved company mappings were applied. The existing CloudFront distribution remains Free/Active; ACM, DNS and application CORS are active.

Chromium checks passed for both actual company identities, SPA refresh, unknown-company rejection, apex guidance and removal of a foreign stored session. Eight CORS origin checks passed. Backend version 3 serves the resolver with no-store responses.

On the Moto G35 5G/Android 15, eight ordinary instrumented tests passed (the opt-in recovery test was initially skipped). The separate deployment test passed against the real resolver and verified that switching M4 to Ramon Lopes clears email/password without persisting company selection before login. The opt-in recovery journey then passed with disposable MongoDB/Mailpit: company selection, captured email, temporary login, mandatory replacement, usable new token and rejected old token. No real user received email. The production API build was restored and installed after the localhost test.

Earlier unchanged-code evidence: backend 477 tests, web 73 tests, database 3 integration tests, Android 68 unit tests. Backend OpenAPI/SDD and web build/contracts passed on release day. Full API Terraform planning remains limited by missing iam:GetRole; targeted Lambda/API, full web and certificate plans all returned No changes. No IAM permissions were changed.

The user authorized commits and pushes after the tests; delivery branch: feat/company-access-login. Login using real company credentials, comprehensive accessibility/backup checks and billing follow-up are not claimed complete. The web CI release variable remains a gate for a later merge/deploy; this feature-branch push does not trigger production deployment.
