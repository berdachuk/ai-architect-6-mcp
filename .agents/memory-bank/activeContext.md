# Active context

**Updated:** 2026-06-16

## Current focus

Bootstrap AI agent context (`.gitignore`, `AGENTS.md`, memory bank, skills, plans). **Next:** M1 — Maven scaffold, Flyway `V1__init_medical_cases.sql`, domain records, `package-info.java`, `ModulithArchitectureTest`.

## Open questions

- Assign formal `REQ-###` IDs across [docs/01-requirements.md](../docs/01-requirements.md) during M1?
- Introduce Cucumber acceptance layer in M5 or keep integration tests only initially?

## Active requirement areas (by doc section)

| Section | Topic | Target milestone |
|---|---|---|
| §5, §9 | Schema + Modulith | M1 |
| §7 | CSV loader | M2 |
| §6 | Retrieval + MCP | M3–M5 |
| §12–§13 | Config | M6 |
| §17 | Quality gates | M3–M4, M8 |

## Risks

| ID | Risk | Mitigation |
|---|---|---|
| RISK-001 | No code yet — agents may invent structure | Follow [docs/03-design.md](../docs/03-design.md) + nested `AGENTS.md` |
| RISK-002 | Embedding endpoint unavailable at startup | Fail fast; document Ollama prerequisite |
| RISK-003 | Quality threshold overfit on test split | Tune on validation only; gate on test |

## Provisional / unverified

- All module boundaries derived from **docs only** — validate with `ApplicationModules.verify()` at M1.

## Next steps

1. Create `pom.xml` and Boot stub per [docs/03-design.md](../docs/03-design.md)
2. Add `V1__init_medical_cases.sql` per [docs/01-requirements.md §5](../docs/01-requirements.md#5-database-schema)
3. Write `ModulithArchitectureTest` + `FlywaySchemaIntegrationTest` first (TDD)
