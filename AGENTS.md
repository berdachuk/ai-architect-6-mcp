# medical-mcp-server — Agent guide

Spring AI 2.0 MCP server over the [HPE medical cases dataset](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial). Single Maven module, Spring Modulith package boundaries, JDBC only, SSE on `:8092`.

**Canonical docs:** [docs/README.md](docs/README.md) — read `01-requirements` → `05-deployment` before substantial work.

## Repo map (planned)

```text
.
├── AGENTS.md
├── docs/                    # SRS, SAD, SDD, test plan, deployment
│   └── guides/              # User how-to (MCP, prompt-lab, LM Studio)
├── .agents/
│   ├── memory-bank/         # Session continuity (read first)
│   ├── skills/              # Domain skills (single source of truth)
│   └── plans/               # Milestone plans (M-NN-*.md)
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

## Commands (when scaffold exists)

| Command | Purpose |
|---|---|
| `mvn test` | Unit + Modulith (`ModulithArchitectureTest`) |
| `mvn verify -Pintegration` | + Testcontainers IT |
| `mvn verify -Pquality` | + test-split retrieval benchmarks |
| `mvn verify -Pprompt-lab` | + prompt-lab eval + gate IT |
| `mvn verify -Pprompt-lab-quality` | + retrieval and prompt-lab test-split gates |
| `mvn spring-boot:run` | Local server `:8092/sse` |

> **Windows:** run Maven and Docker/Testcontainers from **WSL** — see [techContext.md](.agents/memory-bank/techContext.md).

## Global boundaries

| | Rule |
|---|---|
| ✅ | Interface in `service/` / `repository/`; `@Service`/`@Repository` on `impl/` only |
| ✅ | MCP layer injects interfaces only — no JDBC in `mcp/` |
| ✅ | Dataset-backed MCP surface only (5 tools, 2 resources, 1 prompt, 0 completions) |
| ✅ | TDD: test first → requirement alignment → security pre-check → implement → `mvn verify` |
| ⚠️ | Embedding pool mandatory; `nomic-embed-text:v1.5` @ 768 dims |
| ⚠️ | Quality gates tuned on **validation**, locked on **test** split |
| 🚫 | JPA/Hibernate, `@McpComplete`, REST API (default profile), secrets in repo |
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

| File | Purpose |
|---|---|
| [projectbrief.md](.agents/memory-bank/projectbrief.md) | Identity, scope, stakeholders |
| [activeContext.md](.agents/memory-bank/activeContext.md) | Current focus, risks, next steps |
| [systemPatterns.md](.agents/memory-bank/systemPatterns.md) | Modules, domain ownership, traceability |
| [techContext.md](.agents/memory-bank/techContext.md) | Stack, commands, infra |
| [productContext.md](.agents/memory-bank/productContext.md) | Product intent, MCP surface |
| [progress.md](.agents/memory-bank/progress.md) | Dated work log |
| [decisions.md](.agents/memory-bank/decisions.md) | ADR-style decision log |

**After each task:** update `activeContext.md` + `progress.md`; update `decisions.md` / `systemPatterns.md` if architecture changed.

## Traceability

- Requirements source: [docs/01-requirements.md](docs/01-requirements.md) (sections §2–§18).
- Use `REQ-###` / `NFR-###` / `SCN-###` / `TEST-###` / `DEC-###` when implementing; map to owning Modulith package.
- Behavior changes: update tests + traceability notes before or with code.
- Load [bdd-traceability](.agents/skills/bdd-traceability/SKILL.md) for acceptance specs.

## AI context strategy

Layer model and maintenance rules: [docs/ai-context-strategy.md](docs/ai-context-strategy.md).

## Milestones

Canonical table: [docs/01-requirements.md §14](docs/01-requirements.md#14-milestones). Plans: [.agents/plans/00-index.md](.agents/plans/00-index.md).

**Current status:** M-15 prompt-lab live chat complete. Optional M-16 CI combined gates next ([M-16 plan](.agents/plans/M-16-ci-combined-quality-gates.md)). User guides: [docs/guides/README.md](docs/guides/README.md).
