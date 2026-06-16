# M-05 — Embedding module (M4)

**Status:** ⬜ Planned  
**Date:** 2026-06-16  
**Milestone:** M4 — [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones)

## Objective

Mandatory `EmbeddingEndpointPool`, `EmbeddingService`, dataset pass-2 embedding backfill, and pgvector semantic search.

## Deliverables

| Artifact | Path |
|---|---|
| Multi-endpoint properties | `embedding/config/MultiEndpointEmbeddingProperties.java` |
| Endpoint pool | `embedding/multiendpoint/EmbeddingEndpointPool.java` |
| Pool config | `embedding/config/EmbeddingEndpointPoolConfig.java` |
| Embedding service | `embedding/service/EmbeddingService.java` + impl |
| Repository batch update | `MedicalCaseRepositoryImpl.updateEmbeddingsBatch` |
| Loader pass 2 | `DatasetLoaderServiceImpl` embedding pass |
| Semantic search | `VectorSearchServiceImpl.semanticSearch` |
| Integration tests | WireMock embed IT, embedding batch IT |

## Requirement traceability

| ID | Summary | Module | Test |
|---|---|---|---|
| REQ-004 | Embedding model + pool | `embedding`, `dataset` | Embedding IT |
| REQ-006 | Semantic search backend | `retrieval`, `embedding` | Semantic quality subset |

## Acceptance criteria

- [ ] Startup fails when `endpoints` is empty
- [ ] All embed paths route through `EmbeddingEndpointPool`
- [ ] Pass-2 loader fills `VECTOR(768)` for all rows
- [ ] `semanticSearch` returns ordered `SemanticMatch` list
- [ ] `mvn verify -Pintegration` passes

## References

- [docs/03-design.md § Embedding](../../docs/03-design.md)
- [med-expert-match-ce EmbeddingEndpointPool](https://github.com/berdachuk/med-expert-match-ce)
