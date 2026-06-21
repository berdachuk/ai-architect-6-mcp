# medical-mcp-server — Agent guide

Spring AI 2.0 MCP server over the [HPE medical cases dataset](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial). Single Maven module, Spring Modulith package boundaries, JDBC only, SSE on `:8092`.

**Canonical docs:** [docs/README.md](docs/README.md) — read `01-requirements` → `05-deployment` before substantial work.

## Repo map

```text
.
├── AGENTS.md
├── docs/                    # SRS, SAD, SDD, test plan, deployment
│   └── guides/              # User how-to (MCP, prompt-lab, LM Studio)
├── .agents/
│   ├── memory-bank/         # Multi-agent-safe memory (registries + records)
│   ├── skills/              # Domain skills (single source of truth)
│   └── plans/               # Milestone plans (M-NN-*.md)
├── scripts/
│   └── sync-memory-index.sh # Regenerate generated index files (--check for CI)
└── src/main/java/com/example/medicalmcp/
    ├── core/                # Config, security, Flyway, utilities
    ├── medicalcase/         # Domain records + MedicalCaseRepository
    ├── embedding/           # EmbeddingService, EmbeddingEndpointPool
    ├── retrieval/           # VectorSearchService, FTS, stats
    ├── dataset/             # DatasetLoaderService (CSV → JDBC)
    ├── mcp/                 # @McpTool / @McpResource / @McpPrompt
    └── system/              # Actuator health (optional)
```

**Dependency flow:** `mcp` → `retrieval`, `dataset`, `embedding`, `medicalcase` → `core`. `embedding` → `core` only. See [docs/02-architecture.md](docs/02-architecture.md).

## Commands

| Command | Purpose |
|---|---|
| `mvn test` | Unit + Modulith (`ModulithArchitectureTest`) |
| `mvn verify -Pintegration` | + Testcontainers IT |
| `mvn verify -Pquality` | + test-split retrieval benchmarks |
| `mvn verify -Pprompt-lab` | + prompt-lab eval + gate IT |
| `mvn verify -Pprompt-lab-quality` | + retrieval and prompt-lab test-split gates |
| `mvn spring-boot:run` | Local server `:8092/sse` |
| `scripts/sync-memory-index.sh` | Regenerate memory-bank indexes |
| `scripts/sync-memory-index.sh --check` | CI gate: fail if indexes stale |

> **Windows:** run Maven and Docker/Testcontainers from **WSL** — see [techContext.md](.agents/memory-bank/techContext.md).

## Global boundaries

| | Rule |
|---|---|
| ✅ | Interface in `service/` / `repository/`; `@Service`/`@Repository` on `impl/` only |
| ✅ | MCP layer injects interfaces only — no JDBC in `mcp/` |
| ✅ | Dataset-backed MCP surface only (5 tools, 2 resources, 1 prompt, 0 completions) |
| ✅ | TDD: test first → requirement alignment → security pre-check → implement → `mvn verify` |
| ✅ | Append-only to `registry/*.jsonl` + `records/**/*.md`; run `sync-memory-index.sh` after |
| ⚠️ | Embedding pool mandatory; `nomic-embed-text:v1.5` @ 768 dims |
| ⚠️ | Quality gates tuned on **validation**, locked on **test** split |
| ⚠️ | Acquire `locks/<module>.md` before editing coupled file pairs |
| 🚫 | JPA/Hibernate, `@McpComplete`, REST API (default profile), secrets in repo |
| 🚫 | Hand-edit generated indexes (`activeContext`, `progress`, `decisions`, `productContext` tables, `plans/00-index`) |
| 🚫 | Edit an existing registry line or bundle multiple milestones in one record file |
| 🚫 | Change Modulith `allowedDependencies` without updating docs + memory bank |

## Module guidance (nested AGENTS.md)

| Module | Path | Owns |
|---|---|---|
| MCP adapters | [mcp/AGENTS.md](src/main/java/com/example/medicalmcp/mcp/AGENTS.md) | Tool/resource/prompt delegation |
| Domain + repo | [medicalcase/AGENTS.md](src/main/java/com/example/medicalmcp/medicalcase/AGENTS.md) | `MedicalCase`, `CaseSummary`, repository |
| Embeddings | [embedding/AGENTS.md](src/main/java/com/example/medicalmcp/embedding/AGENTS.md) | Pool, `EmbeddingService` |
| Retrieval | [retrieval/AGENTS.md](src/main/java/com/example/medicalmcp/retrieval/AGENTS.md) | FTS, semantic search, stats |
| Dataset load | [dataset/AGENTS.md](src/main/java/com/example/medicalmcp/dataset/AGENTS.md) | CSV loader, two-pass ingest |

## Skills index

Load from `.agents/skills/<name>/SKILL.md`.

| Skill | Trigger |
|---|---|
| [core-architecture](.agents/skills/core-architecture/SKILL.md) | Module boundaries, layering, Modulith deps |
| [domain-modeling](.agents/skills/domain-modeling/SKILL.md) | Records, invariants, repository contracts |
| [requirements-modeling](.agents/skills/requirements-modeling/SKILL.md) | REQ/NFR IDs, traceability to modules |
| [db-migrations](.agents/skills/db-migrations/SKILL.md) | Flyway, pgvector schema |
| [testing](.agents/skills/testing/SKILL.md) | TDD, Testcontainers, quality profiles |
| [bdd-traceability](.agents/skills/bdd-traceability/SKILL.md) | Requirements → Gherkin → tests |
| [security-check](.agents/skills/security-check/SKILL.md) | Before/after auth, DB, MCP, deps work |
| [write-less-code](.agents/skills/write-less-code/SKILL.md) | Every implementation pass |

## Memory bank (read at session start)

Multi-agent-safe structure (DEC-012). **Read first:** `projectbrief.md` → `activeContext.md` → `systemPatterns.md` → `techContext.md` → nearest nested `AGENTS.md`.

### Reference files (hand-edited, low-frequency)

| File | Purpose |
|---|---|
| [projectbrief.md](.agents/memory-bank/projectbrief.md) | Identity, scope, stakeholders |
| [systemPatterns.md](.agents/memory-bank/systemPatterns.md) | Modules, domain ownership, traceability |
| [techContext.md](.agents/memory-bank/techContext.md) | Stack, commands, infra |
| [productContext.md](.agents/memory-bank/productContext.md) | Product intent, MCP surface (prose hand-edited; tables generated) |

### Generated index files (read-only — do NOT hand-edit)

Regenerated by `scripts/sync-memory-index.sh` from registries + records. `--check` is a CI gate.

| File | Source |
|---|---|
| [activeContext.md](.agents/memory-bank/activeContext.md) | `records/active/*.md` + `registry/risk.jsonl` + `registry/scn.jsonl` |
| [progress.md](.agents/memory-bank/progress.md) | `records/progress/*.md` (one per milestone) |
| [decisions.md](.agents/memory-bank/decisions.md) | `registry/dec.jsonl` |
| [productContext.md](.agents/memory-bank/productContext.md) tables | `registry/req.jsonl` + `scn.jsonl` + `test.jsonl` |
| [plans/00-index.md](.agents/plans/00-index.md) | `records/active/` + `records/deferred/` + `records/progress/` |

### Append-only registries (multi-agent safe ID allocation)

One JSON object per line in `.agents/memory-bank/registry/`. Schema: [registry/SCHEMA.md](.agents/memory-bank/registry/SCHEMA.md). To mint an ID: read, take `max+1`, append one line. Never edit an existing line.

| File | ID kind |
|---|---|
| `req.jsonl` | `REQ-###` functional requirements |
| `nfr.jsonl` | `NFR-###` non-functional requirements |
| `scn.jsonl` | `SCN-###` behavior scenarios |
| `test.jsonl` | `TEST-###` test artifacts |
| `dec.jsonl` | `DEC-###` decisions (index; body in `records/decisions/`) |
| `risk.jsonl` | `RISK-###` known risks |
| `task.jsonl` | `TASK-###` plan tasks |

### Per-record files (append-only, one file per record)

`records/progress/M{NN}.md` (completed milestone), `records/active/M{NN}.md` (active), `records/deferred/M{NN}.md`, `records/decisions/DEC-###.md` (decision body). Two agents completing different milestones create distinct files → zero merge conflict.

### Module locks & worktree scratchpads

- `locks/<module>.md` — acquire before editing coupled file pairs (prompt + sanitizer). Format + rules: [locks/README.md](.agents/memory-bank/locks/README.md)
- `worktrees/<branch-slug>/` — per-branch scratchpad, git-ignored. Never merge to main.

**After each task:** update `records/active/M{NN}.md` (or move to `records/progress/`); append registry rows; run `scripts/sync-memory-index.sh`.

## Traceability

- Requirements source: [docs/01-requirements.md](docs/01-requirements.md) (sections §2–§18).
- Stable IDs in `registry/*.jsonl`: `REQ-###` / `NFR-###` / `SCN-###` / `TEST-###` / `DEC-###` / `RISK-###` / `TASK-###`.
- Traceability tables generated in [productContext.md](.agents/memory-bank/productContext.md).
- Behavior changes: update tests + registry rows before or with code.
- Load [bdd-traceability](.agents/skills/bdd-traceability/SKILL.md) for acceptance specs.

## AI context strategy

Layer model and maintenance rules: [docs/ai-context-strategy.md](docs/ai-context-strategy.md).

## Milestones

Canonical table: [docs/01-requirements.md §14](docs/01-requirements.md#14-milestones). Plans index: [.agents/plans/00-index.md](.agents/plans/00-index.md). Current status in [activeContext.md](.agents/memory-bank/activeContext.md).