# Progress log

## 2026-06-16 — Docs v2.0.0

- Reorganized documentation into numbered pipeline: `01-requirements` → `05-deployment`
- Committed `4caab79` on `develop`

## 2026-06-16 — AI context bootstrap

- Added `.gitignore`, root `AGENTS.md`
- Created `.agents/memory-bank/`, `.agents/skills/`, `.agents/plans/`
- Added nested module `AGENTS.md` (5 packages)
- Added `docs/ai-context-strategy.md`
- **Modules touched:** (docs/agents only)
- **Plan archived:** `.agents/plans/archive/M-01-ai-context-foundation.md`
- **Tests:** none
- **Traceability:** foundation for M1+ implementation

## 2026-06-16 — M1 Modulith foundation

- Added `pom.xml`, `MedicalMcpApplication`, 7 Modulith modules, 5 domain records
- Flyway `V1__init_medical_cases.sql` (pgvector, HNSW, FTS)
- Tests: `ModulithArchitectureTest`, `FlywaySchemaIntegrationTest` — all pass
- **REQ-005, REQ-009** · modules: all · plan archived: `M-02-modulith-foundation.md`

## 2026-06-16 — M2 Dataset loader

- `MedicalCaseRepository` (insert, findById, countAll), `DatasetLoaderService` pass 1
- `train-sample-10.csv` fixture, `DatasetLoaderIntegrationTest`
- Modulith `@NamedInterface` on medicalcase domain/repository packages
- **REQ-005, REQ-007** · plan archived: `M-03-dataset-loader.md`

## 2026-06-16 — M3 Retrieval module

- `MedicalCaseRepository` FTS, `listSpecialties`, `countBySplit`
- `VectorSearchService` + Caffeine stats cache, limit clamp
- IT: repository, stats, `FtsRetrievalQualityIntegrationTest` subset
- Singleton Testcontainers Postgres for stable WSL/Docker IT
- **REQ-006** · plan archived: `M-04-retrieval-module.md`

## 2026-06-16 — M4 Embedding module

- `@InjectSql`, SQL files, `EmbeddingEndpointPool`, `EmbeddingService`, loader pass 2
- `semanticSearch` via pgvector; `SemanticRetrievalQualityIntegrationTest` subset
- **REQ-004, REQ-006, DEC-010, DEC-011** · plan archived: `M-05-embedding-module.md`

## Milestone status

| Milestone | Status |
|---|---|
| M1–M4 | ✅ Complete |
| M5–M8 | ⬜ Not started |
| M9–M10 | ⬜ Future scope |

Canonical milestone table: [docs/01-requirements.md §14](../docs/01-requirements.md#14-milestones)
