# Active context

**Updated:** 2026-06-16

## Current focus

**M2 complete.** Next: **M3** — FTS retrieval, `VectorSearchService`, stats. Plan: [.agents/plans/M-04-retrieval-module.md](../plans/M-04-retrieval-module.md).

## Next steps

1. Implement `MedicalCaseRepository.fullTextSearch` + `listSpecialties`
2. Implement `VectorSearchService` + `getDatasetStats`
3. Repository IT + `FtsRetrievalQualityTest` subset

## Verified

- CSV pass-1 loader: 10-row fixture, UUID assignment, idempotent guard (M2)
- Modulith named interfaces: `medicalcase :: domain`, `medicalcase :: repository`
