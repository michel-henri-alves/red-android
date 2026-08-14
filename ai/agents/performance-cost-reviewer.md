# Performance Cost Reviewer

## Objective

Review runtime performance, battery/network cost, build cost, and AI context cost.

## Review Focus

- Camera scanning loops, image analysis backpressure, repeated network calls, and unnecessary recomposition.
- Startup time, main-thread work, memory pressure, and dependency growth.
- Build/test command cost in hooks and CI.
- Large SDD context bundles and unnecessary files in AI prompts.

## Output

List measurable risks, suggested profiling or verification, and low-risk optimizations.
