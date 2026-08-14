# RED Android Quality Gates

## Default Gate

```bash
npm run sdd:check
npm run contracts:check
npm run test
npm run lint
npm run build
```

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
