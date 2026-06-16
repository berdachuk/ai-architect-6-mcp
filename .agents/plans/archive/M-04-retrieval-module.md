# M-04 — Retrieval module (M3)

**Status:** ✅ Archived  
**Date:** 2026-06-16  
**Milestone:** M3

## Deliverables

| Artifact | Status |
|---|---|
| `MedicalCaseRepositoryImpl` FTS + `listSpecialties` + `countBySplit` | ✅ |
| `VectorSearchService` + impl (search, stats, limit clamp) | ✅ |
| `RetrievalCacheConfig` — Caffeine 60s stats cache | ✅ |
| Integration tests + FTS quality subset | ✅ |
| Singleton Testcontainers Postgres | ✅ |

## Verification

- `mvn test` — unit + Modulith + limit clamp
- `mvn verify -Pintegration` — repository IT, stats, FTS quality subset (WSL + Docker)

## Next

[M-05 embedding module](M-05-embedding-module.md) (requirements M4).
