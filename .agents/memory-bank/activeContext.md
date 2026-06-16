# Active context

**Updated:** 2026-06-16

## Current focus

**M5 complete.** Next: **M6** — config binding, security, cache TTL refinement. Plan: [.agents/plans/M-07-config-security.md](../plans/M-07-config-security.md).

## Next steps

1. `@ConfigurationProperties` for `medicalmcp.*`
2. `SecurityConfig` — actuator scope
3. Externalize stats cache TTL + binding tests

## Verified

- MCP tools (5), resources (2), `case-analysis` prompt (M5)
- `@InjectSql` in `repository/impl` only; IT `@Sql` cleanup (DEC-010/011)
- `SharedPostgresContainer` + `@Testcontainers` (DEC-009)
- `mvn verify -Pintegration` via WSL
