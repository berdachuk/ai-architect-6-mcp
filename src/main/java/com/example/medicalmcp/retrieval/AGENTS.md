# retrieval module — agent guide

**Package:** `com.example.medicalmcp.retrieval`  
**Modulith deps:** `core`, `medicalcase`, `embedding`

## Purpose

Search and stats: `VectorSearchService` — FTS (`tsvector`), pgvector cosine ANN, specialty listing, dataset statistics (with Caffeine cache).

## Owned behavior

| Capability | MCP tool(s) |
|---|---|
| Full-text search | `search_cases` |
| Semantic search | `semantic_search` |
| Specialty list | `list_specialties` |
| Stats | `get_dataset_stats` |

`get_case` uses `MedicalCaseRepository.findById` — may live in repository or thin delegation via retrieval; keep UUID lookup in repository layer.

## Constraints

- Inject `MedicalCaseRepository` and `EmbeddingService` interfaces only
- Respect `MEDICALMCP_RETRIEVAL_MAX_LIMIT` (default 50)
- Stats cache TTL 60s — `docs/02-architecture.md`
- Quality metrics: tune thresholds on **validation**, gate on **test**

## Skills

- `.agents/skills/testing/SKILL.md` — `FtsRetrievalQualityTest`, `SemanticRetrievalQualityTest`
- `.agents/skills/domain-modeling/SKILL.md`

## Tests (M3–M4)

- Repository FTS IT
- Semantic retrieval quality on test split (profile `quality`)
