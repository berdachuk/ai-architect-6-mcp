# Active context

**Updated:** 2026-06-16

## Current focus

**M6 complete.** Next: **M7** — end-to-end MCP smoke (`McpSyncClient`, Claude Desktop checklist). Plan: [.agents/plans/M-08-e2e-smoke.md](../plans/M-08-e2e-smoke.md).

## Next steps

1. E2E profile / `McpSyncClient` integration against running server
2. Full dataset load smoke (2464 rows) with Ollama embeddings
3. Manual checklist in [docs/04-testing.md §11](../../docs/04-testing.md#11-manual-smoke-checklist-m7)

## Verified

- `RetrievalProperties`, `SecurityConfig`, actuator, cache TTL (M6)
- MCP tools (5), resources (2), `case-analysis` prompt (M5)
- `@InjectSql` in `repository/impl` only; IT `@Sql` cleanup (DEC-010/011)
- `SharedPostgresContainer` + `@Testcontainers` (DEC-009)
- `mvn verify -Pintegration` via WSL
