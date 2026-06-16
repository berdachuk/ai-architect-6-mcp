# M-05 — Embedding module (M4)

**Status:** ✅ Archived  
**Date:** 2026-06-16  
**Milestone:** M4

## Deliverables

| Artifact | Status |
|---|---|
| `@InjectSql` + `SqlInjectBeanPostProcessor` | ✅ |
| SQL files in `sql/medicalcase/` (named `:binds`) | ✅ |
| `EmbeddingEndpointPool` + `EmbeddingService` | ✅ |
| Dataset loader pass 2 | ✅ |
| `VectorSearchService.semanticSearch` | ✅ |
| IT: `EmbeddingLoaderIntegrationTest`, `SemanticRetrievalQualityIntegrationTest` | ✅ |

## Verification

- `mvn test` — unit + Modulith
- `mvn verify -Pintegration` — embedding + semantic quality subset (WSL + Docker)

## Next

[M-06 MCP module](../M-06-mcp-module.md) (requirements M5).
