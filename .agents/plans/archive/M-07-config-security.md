# M-07 — Config + security (M6)

**Status:** ✅ Complete (archived 2026-06-16)  
**Date:** 2026-06-16  
**Milestone:** M6 — [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones)

## Objective

Harden configuration binding, security defaults, and refine operational properties. Partial cache work exists from M3 (`RetrievalCacheConfig`).

## Prerequisites

- [M-06](M-06-mcp-module.md) complete — MCP surface wired

## Deliverables

| Artifact | Path |
|---|---|
| Properties binding | `RetrievalProperties`, `DatasetLoaderProperties`, `MultiEndpointEmbeddingProperties` |
| Security | `core/config/SecurityConfig.java` — MCP + actuator health/info permitAll |
| Jackson | `core/config/JacksonConfig.java` — `ObjectMapper` for MCP JSON resources |
| Cache refinement | `RetrievalCacheConfig` — TTL from `medicalmcp.retrieval.stats-cache-ttl-seconds` |
| Env var docs | `application.yml` aligned with [docs/01-requirements.md §13](../../docs/01-requirements.md#13-environment-variables) |
| Config tests | `RetrievalPropertiesBindingTest`, `DatasetLoaderPropertiesBindingTest`, `EmbeddingPropertiesValidationTest`, `StatsCacheTtlIntegrationTest` |

## Requirement traceability

| ID | Summary | Module | Test |
|---|---|---|---|
| NFR-003 | Config fails fast on invalid properties | `core`, `embedding`, `dataset`, `retrieval` | Binding/validation tests |
| NFR-004 | Security defaults — no public DB | `core` | SecurityConfig |
| REQ-006 | Retrieval limits from config | `retrieval` | `RetrievalPropertiesBindingTest`, MCP IT |

## Acceptance criteria

- [x] Invalid config fails fast at startup (empty embedding endpoints, bad URLs)
- [x] `medicalmcp.retrieval.max-limit` and loader properties bind correctly
- [x] Stats cache TTL configurable; TTL test passes
- [x] `mvn test` and `mvn verify -Pintegration` pass (WSL)

## Also shipped

- `spring-boot-starter-actuator`, `spring-boot-starter-security` in `pom.xml`
- `VectorSearchServiceImpl` injects `RetrievalProperties` (no `@Value`)
- MCP resources return JSON `String` (Spring AI 2.0); invalid UUID guarded in tools/prompts

## References

- [docs/05-deployment.md](../../docs/05-deployment.md)
- [docs/02-architecture.md](../../docs/02-architecture.md)

## Next

[M-08 end-to-end smoke](../M-08-e2e-smoke.md) (requirements M7).
