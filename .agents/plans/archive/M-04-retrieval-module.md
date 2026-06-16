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
| Inline SQL in repository impl | ✅ (migrate in M-05 per [DEC-010](../memory-bank/decisions.md#dec-010--external-sql-files-with-injectsql), [DEC-011](../memory-bank/decisions.md#dec-011--named-bind-variables-in-sql)) |
| Dynamic SQL `StringBuilder` for FTS filters | ✅ (replace with single `fullTextSearch.sql` + `COALESCE(:param,'')` optional filters in M-05) |

## Verification

- `mvn test` — unit + Modulith + limit clamp
- `mvn verify -Pintegration` — repository IT, stats, FTS quality subset (WSL + Docker)

## Next

[M-05 embedding module](../M-05-embedding-module.md) (requirements M4).
