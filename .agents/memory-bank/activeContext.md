# Active context

**Updated:** 2026-06-16

## Current focus

**M7 complete.** Next: **M8** — Docker Compose, CI, quality gate on test split. Plan: [.agents/plans/M-09-docker-quality-gate.md](../plans/M-09-docker-quality-gate.md).

## Next steps

1. `Dockerfile` + `docker-compose.yml` (PG + server; Ollama on host)
2. GitHub Actions: `mvn test`, `mvn verify -Pintegration`
3. `quality` Maven profile — full test-split retrieval benchmarks

## Verified

- `McpSseSmokeIntegrationTest` — all 5 tools, 2 resources, `case-analysis` prompt over SSE (M7)
- `RetrievalProperties`, `SecurityConfig`, actuator, cache TTL (M6)
- MCP contract ITs (M5); `@InjectSql` / Testcontainers patterns (DEC-009–011)
- `mvn verify -Pintegration` via WSL
