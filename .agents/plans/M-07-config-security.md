# M-07 — Config + security (M6)

**Status:** ⬜ Planned  
**Date:** 2026-06-16  
**Milestone:** M6 — [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones)

## Objective

Harden configuration binding, security defaults, and refine operational properties. Partial cache work exists from M3 (`RetrievalCacheConfig`).

## Prerequisites

- [M-06](M-06-mcp-module.md) complete — MCP surface wired

## Deliverables

| Artifact | Path |
|---|---|
| Properties binding | `@ConfigurationProperties` for `medicalmcp.*` (dataset, retrieval, embedding) |
| Security | `core/config/SecurityConfig.java` — actuator scope, no public DB exposure |
| Cache refinement | Externalize Caffeine TTL (`medicalmcp.retrieval.stats-cache-ttl`); cache TTL integration test |
| Env var docs | Align `application.yml` with [docs/01-requirements.md §13](../../docs/01-requirements.md#13-environment-variables) |
| Config tests | Property binding tests, cache TTL test |

## Acceptance criteria

- [ ] Invalid config fails fast at startup (empty embedding endpoints, bad URLs)
- [ ] `medicalmcp.retrieval.max-limit` and loader properties bind correctly
- [ ] Stats cache TTL configurable; TTL test passes
- [ ] `mvn test` passes

## References

- [docs/05-deployment.md](../../docs/05-deployment.md)
- [docs/02-architecture.md](../../docs/02-architecture.md)

## Next

[M-08 end-to-end smoke](M-08-e2e-smoke.md) (requirements M7).
