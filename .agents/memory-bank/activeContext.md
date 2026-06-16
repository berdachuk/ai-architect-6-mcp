# Active context

**Updated:** 2026-06-16

## Current focus

**M8 complete.** Next optional: **M9** prompt lab. Plan: [.agents/plans/M-10-prompt-lab.md](../plans/M-10-prompt-lab.md).

Core milestone track (M1–M8) is complete. M9/M10 are optional per [docs/01-requirements.md §18](../../docs/01-requirements.md#18-future-scope-optional).

## Next steps (optional M9)

1. `prompt-lab` Maven profile scaffold
2. Offline eval harness for `case-analysis` template variants
3. Metrics alongside `quality-report.json`

## Verified

- Docker Compose + `Dockerfile`; GitHub Actions CI + nightly quality workflow (M8)
- `RetrievalQualityGateIntegrationTest` + `target/test-output/quality-report.json`
- `McpSseSmokeIntegrationTest` (M7); config/security (M6); MCP surface (M5)
- `mvn verify -Pintegration` / `-Pquality` via WSL
