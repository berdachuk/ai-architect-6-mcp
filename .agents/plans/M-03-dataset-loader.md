# M-03 — Dataset loader (M2)

**Status:** ⬜ Planned  
**Date:** 2026-06-16  
**Milestone:** M2 — [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones)

## Objective

Load HuggingFace CSV splits into PostgreSQL via `DatasetLoaderService` and `MedicalCaseRepository` (insert pass).

## Deliverables

| Artifact | Path |
|---|---|
| Repository API | `medicalcase/repository/MedicalCaseRepository.java` |
| Repository impl | `medicalcase/repository/impl/MedicalCaseRepositoryImpl.java` |
| Loader service | `dataset/service/DatasetLoaderService.java` + impl |
| SQL | `src/main/resources/sql/medicalcase/` (optional) |
| Test fixture | `src/test/resources/dataset/train-sample-10.csv` |
| Integration test | `DatasetLoaderIntegrationTest` |

## Requirement traceability

| ID | Summary | Module | Test |
|---|---|---|---|
| REQ-007 | CSV data loading | `dataset`, `medicalcase` | `DatasetLoaderIntegrationTest` |
| REQ-005 | Persist HF schema columns | `medicalcase` | loader IT + round-trip |

## Acceptance criteria

- [ ] Load `train-sample-10.csv` — 10 rows, correct `split=train`
- [ ] Server assigns UUID per row
- [ ] Idempotent: second run does not duplicate (`COUNT(*) > 0` guard)
- [ ] `mvn verify -Pintegration` passes

## References

- [docs/01-requirements.md §7](../../docs/01-requirements.md#7-data-loading)
- [docs/03-design.md](../../docs/03-design.md)
- [docs/04-testing.md §5.3](../../docs/04-testing.md)
