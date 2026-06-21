# Plans index

**Generated** by `scripts/sync-memory-index.sh` — do not hand-edit. Canonical milestone table: [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones).

## Active
| Plan | Milestone | Status |
|---|---|---|
| [record](../memory-bank/records/active/M-16.md) | M-16 — CI combined quality gates (active) | ⬜ Active |

## Deferred
| Plan | Milestone |
|---|---|
| _none_ | |

## Completed
| Plan | Milestone | Record |
|---|---|---|
| [archive/M-01-ai-context-foundation.md](archive/M-01-ai-context-foundation.md) | M-01 — AI context foundation | [record](../memory-bank/records/progress/M-01.md) |
| [archive/M-02-modulith-foundation.md](archive/M-02-modulith-foundation.md) | M1 — Modulith foundation | [record](../memory-bank/records/progress/M02.md) |
| [archive/M-03-dataset-loader.md](archive/M-03-dataset-loader.md) | M2 — Dataset loader | [record](../memory-bank/records/progress/M03.md) |
| [archive/M-04-retrieval-module.md](archive/M-04-retrieval-module.md) | M3 — Retrieval module | [record](../memory-bank/records/progress/M04.md) |
| [archive/M-05-embedding-module.md](archive/M-05-embedding-module.md) | M4 — Embedding module | [record](../memory-bank/records/progress/M05.md) |
| [archive/M-06-mcp-module.md](archive/M-06-mcp-module.md) | M5 — MCP module + SQL/IT alignment | [record](../memory-bank/records/progress/M06.md) |
| [archive/M-07-config-security.md](archive/M-07-config-security.md) | M6 — Config + security | [record](../memory-bank/records/progress/M07.md) |
| [archive/M-08-e2e-smoke.md](archive/M-08-e2e-smoke.md) | M7 — End-to-end smoke | [record](../memory-bank/records/progress/M08.md) |
| [archive/M-09-docker-quality-gate.md](archive/M-09-docker-quality-gate.md) | M8 — Docker + quality gate | [record](../memory-bank/records/progress/M09.md) |
| [archive/M-10-prompt-lab.md](archive/M-10-prompt-lab.md) | M9 — Prompt lab (optional) | [record](../memory-bank/records/progress/M10.md) |
| [archive/M-11-prompt-integration.md](archive/M-11-prompt-integration.md) | M10 — Prompt integration (optional) | [record](../memory-bank/records/progress/M11.md) |
| [archive/M-12-meta-prompting-lab.md](archive/M-12-meta-prompting-lab.md) | M12 — Meta-prompting lab (M9 ext) | [record](../memory-bank/records/progress/M12.md) |
| [archive/M-13-user-guides.md](archive/M-13-user-guides.md) | M13 — User guides (docs) | [record](../memory-bank/records/progress/M13.md) |
| [archive/M-14-claude-desktop-mcp.md](archive/M-14-claude-desktop-mcp.md) | M14 — Claude Desktop guide (docs) | [record](../memory-bank/records/progress/M14.md) |
| [archive/M-15-prompt-lab-live-chat.md](archive/M-15-prompt-lab-live-chat.md) | M-15 — Prompt-lab live chat + test gate | [record](../memory-bank/records/progress/M15.md) |
| [archive/M-17-mcp-self-description-improvements.md](archive/M-17-mcp-self-description-improvements.md) | M-17 — MCP self-description improvements | [record](../memory-bank/records/progress/M17.md) |
| [archive/M-18-mongodb-compatible-string-ids.md](archive/M-18-mongodb-compatible-string-ids.md) | M-18 — MongoDB-compatible string IDs | [record](../memory-bank/records/progress/M18.md) |

## Conventions
- Filename: `M-NN-short-topic.md` (zero-padded `NN`; plan `M-0N` maps to requirements milestone `M(N-1)` for M1+)
- Link requirement sections and test deliverables from [docs/04-testing.md §10](../../docs/04-testing.md#10-mapping-to-milestones)
- Update `records/progress/M{NN}.md` when a plan completes; run `scripts/sync-memory-index.sh` to refresh this index.

## SQL conventions (DEC-010, DEC-011)
- Files: `src/main/resources/sql/{module}/*.sql`
- Injection: `@InjectSql("/sql/medicalcase/findById.sql") String findByIdSql;` in `*/repository/impl/*`
- Binds: **`:name` only** — no positional `?`; optional filters via `COALESCE(:param, '')` in SQL, not Java string building
- Infra: `InjectSql` + `SqlInjectBeanPostProcessor` in `core`
- IT cleanup: Spring `@Sql` scripts (not repository API)

## Tests (DEC-009)
- `mvn test` — unit + Modulith (no Docker)
- `mvn verify -Pintegration` — Testcontainers; run from **WSL** on Windows
- `mvn verify -Pquality` — full test-split benchmarks (M8+)
- `mvn verify -Pprompt-lab` — prompt-lab eval + MCP tools IT
- `mvn verify -Pprompt-lab-quality` — retrieval + prompt-lab test-split gates

## User guides
End-user how-to: [docs/guides/README.md](../../docs/guides/README.md)
