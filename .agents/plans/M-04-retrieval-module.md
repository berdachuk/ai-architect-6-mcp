# M-04 — Retrieval module (M3)

**Status:** ⬜ Planned  
**Date:** 2026-06-16  
**Milestone:** M3 — [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones)

## Objective

FTS search, specialty listing, and dataset stats via `VectorSearchService` and completed `MedicalCaseRepository` queries.

## Deliverables

| Artifact | Path |
|---|---|
| Repository FTS + specialties | `MedicalCaseRepositoryImpl` — `fullTextSearch`, `listSpecialties` |
| Retrieval service | `retrieval/service/VectorSearchService.java` + impl |
| Stats + cache | `DatasetStats` aggregation, Caffeine TTL (M6 may refine config) |
| Integration tests | Repository IT, `FtsRetrievalQualityTest` (subset) |

## Requirement traceability

| ID | Summary | Module | Test |
|---|---|---|---|
| REQ-006 | MCP search/stats (backend) | `retrieval`, `medicalcase` | Repository IT, FTS quality subset |

## Acceptance criteria

- [ ] `fullTextSearch` respects specialty, split, limit
- [ ] `listSpecialties` returns 13 exact HF labels with counts
- [ ] `getDatasetStats` returns total + bySpecialty + bySplit
- [ ] `mvn verify -Pintegration` passes

## References

- [docs/03-design.md](../../docs/03-design.md)
- [docs/04-testing.md §5–6](../../docs/04-testing.md)
