# Active context

**Updated:** 2026-06-16

## Current focus

**M3 complete.** Next: **M4** — `EmbeddingEndpointPool`, pass-2 loader, semantic search. Plan: [.agents/plans/M-05-embedding-module.md](../plans/M-05-embedding-module.md).

## Next steps

1. Implement `MultiEndpointEmbeddingProperties` + `EmbeddingEndpointPool`
2. `EmbeddingService` + manual `OpenAiEmbeddingModel` wiring
3. Dataset pass-2 + `updateEmbeddingsBatch` + `semanticSearch`

## Verified

- FTS search with specialty/split filters and limit clamp (M3)
- `VectorSearchService.getDatasetStats` with Caffeine 60s cache
- Integration tests via WSL + Docker (singleton Testcontainers)
