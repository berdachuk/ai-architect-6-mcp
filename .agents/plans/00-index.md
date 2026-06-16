# Plans index

Milestone implementation plans for AI-assisted development. Canonical milestone table: [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones).

| Plan | Milestone | Status |
|---|---|---|
| [archive/M-01-ai-context-foundation.md](archive/M-01-ai-context-foundation.md) | Bootstrap | ✅ Archived |
| [archive/M-02-modulith-foundation.md](archive/M-02-modulith-foundation.md) | M1 | ✅ Archived |
| [archive/M-03-dataset-loader.md](archive/M-03-dataset-loader.md) | M2 | ✅ Archived |
| [archive/M-04-retrieval-module.md](archive/M-04-retrieval-module.md) | M3 | ✅ Archived |
| [archive/M-05-embedding-module.md](archive/M-05-embedding-module.md) | M4 | ✅ Archived |
| [M-06-mcp-module.md](M-06-mcp-module.md) | M5 | ⬜ **Active** |
| [M-07-config-security.md](M-07-config-security.md) | M6 | ⬜ Planned |
| [M-08-e2e-smoke.md](M-08-e2e-smoke.md) | M7 | ⬜ Planned |
| [M-09-docker-quality-gate.md](M-09-docker-quality-gate.md) | M8 | ⬜ Planned |

**Chain:** M-06 → M-07 → M-08 → M-09

Completed plans are moved to `archive/` when no longer active.

## Conventions

- Filename: `M-NN-short-topic.md` (zero-padded `NN`; plan `M-0N` maps to requirements milestone `M(N-1)` for M1+)
- Link requirement sections and test deliverables from [docs/04-testing.md §10](../../docs/04-testing.md#10-mapping-to-milestones)
- Update [.agents/memory-bank/progress.md](../memory-bank/progress.md) when a plan completes

### SQL ([DEC-010](../memory-bank/decisions.md#dec-010--external-sql-files-with-injectsql), [DEC-011](../memory-bank/decisions.md#dec-011--named-bind-variables-in-sql))

- Files: `src/main/resources/sql/{module}/*.sql`
- Injection: `@InjectSql("/sql/medicalcase/findById.sql") String findByIdSql;` in `*/repository/impl/*`
- Binds: **`:name` only** — no positional `?`; optional filters via `COALESCE(:param, '')` in SQL, not Java string building
- Infra: `InjectSql` + `SqlInjectBeanPostProcessor` in `core`

### Tests ([DEC-009](../memory-bank/decisions.md#dec-009--wsl-for-docker-on-windows))

- `mvn test` — unit + Modulith (no Docker)
- `mvn verify -Pintegration` — Testcontainers; run from **WSL** on Windows
- `mvn verify -Pquality` — full test-split benchmarks (M8+)

## Optional (future)

| Plan | Milestone | Reference |
|---|---|---|
| M-10 prompt-lab | M9 | [docs/future/prompt-lab.md](../../docs/future/prompt-lab.md) |
| M-11 prompt integration | M10 | [docs/01-requirements.md §18](../../docs/01-requirements.md#18-future-scope-optional) |
