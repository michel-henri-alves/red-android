# RED Android Quality Gates

## Default Gate

```bash
npm run sdd:check
npm run contracts:check
npm run quality:check
npm run build
```

`quality:check` runs JVM unit tests, produces JaCoCo XML/HTML coverage reports,
and runs Android Lint. Use the narrower commands while developing:

```bash
npm run test
npm run test:coverage
npm run static:analysis
```

Coverage reports are written to `app/build/reports/jacoco/jacocoTestReport/`.

## Optional Device Gate

Use when the feature touches camera, permissions, navigation, or UI behavior that cannot be proven with JVM tests:

```bash
npm run connected:test
```

## Evidence

Record gate results with:

```bash
npm run sdd:run -- NNNN-feature-slug
```
