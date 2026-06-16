# M-03 — Dataset loader (M2)

**Status:** ✅ Archived  
**Date:** 2026-06-16  
**Milestone:** M2

## Deliverables

| Artifact | Status |
|---|---|
| `MedicalCaseRepository` + JDBC impl (insert, findById, countAll) | ✅ |
| `DatasetLoaderService` + impl (pass 1 CSV ingest) | ✅ |
| `DatasetLoaderConfig` + properties | ✅ |
| `train-sample-10.csv` fixture | ✅ |
| `DatasetLoaderIntegrationTest` | ✅ |
| Modulith `@NamedInterface` for `domain` / `repository` | ✅ |

## Verification

- `mvn test` — Modulith + split inference unit test
- `mvn verify -Pintegration` — loader IT (10 rows, UUID, idempotent)

## Next

[M-04 retrieval module](M-04-retrieval-module.md) (requirements M3).
