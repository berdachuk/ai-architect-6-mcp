# Active context

**Updated:** 2026-06-16

## Current focus

**M1 complete.** Next: **M2** — `MedicalCaseRepository`, `DatasetLoaderService`, CSV ingest. Plan: [.agents/plans/M-03-dataset-loader.md](../plans/M-03-dataset-loader.md).

## Open questions

- Introduce Cucumber acceptance layer in M5 or keep integration tests only initially?

## Active requirement areas

| Section | Topic | Target milestone |
|---|---|---|
| §7 | CSV loader | M2 ← **next** |
| §6 | Retrieval + MCP | M3–M5 |
| §12–§13 | Config | M6 |
| §17 | Quality gates | M3–M4, M8 |

## Risks

| ID | Risk | Mitigation |
|---|---|---|
| RISK-002 | Embedding endpoint unavailable at startup | M4 — fail fast; document Ollama prerequisite |
| RISK-003 | Quality threshold overfit on test split | Tune on validation only; gate on test |

## Verified

- Modulith boundaries validated by `ModulithArchitectureTest` (M1)
- Flyway V1 schema validated on pgvector/pg17 (M1)

## Next steps

1. Implement `MedicalCaseRepository` + JDBC impl (TDD)
2. Implement `DatasetLoaderService` + `train-sample-10.csv` fixture
3. `DatasetLoaderIntegrationTest`
