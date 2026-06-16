# Product context

## User-facing capabilities (MCP)

| Kind | Count | Purpose |
|---|---:|---|
| Tools | 5 | FTS search, UUID lookup, semantic search, specialty list, dataset stats |
| Resources | 2 | Case by UUID, stats snapshot |
| Prompts | 1 | `case-analysis` — dataset field focus only |
| Completions | 0 | Removed — incompatible with UUID identity |

Detail: [docs/01-requirements.md §6](../docs/01-requirements.md#6-mcp-surface)

## Dataset constraints

- **5 HF columns only** + server columns (`id`, `split`, `embedding`, `fts`, `created_at`)
- **13 exact** `medical_specialty` labels — filters must match verbatim
- `sample_name` is **not unique** — use UUID for identity
- `keywords` nullable (~36% empty)

Detail: [docs/01-requirements.md §2](../docs/01-requirements.md#2-source-dataset)

## Use cases

Workflow catalog: [docs/use-cases.md](../docs/use-cases.md)

## Non-goals

No clinical inference beyond stored text; no production specialty classifier API. See [docs/01-requirements.md §3](../docs/01-requirements.md#3-goals--non-goals).

## Success criteria

- Modulith `verify()` passes in CI
- MCP contract tests pass
- FTS + semantic quality gates on **test** split per [docs/04-testing.md §6](../docs/04-testing.md)
