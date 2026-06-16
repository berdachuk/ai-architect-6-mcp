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

## Milestone status

| Milestone | Status |
|---|---|
| M1 | ✅ Complete |
| M2–M8 | ⬜ Not started |
| M9–M10 | ⬜ Future scope |

Canonical milestone table: [docs/01-requirements.md §14](../docs/01-requirements.md#14-milestones)
