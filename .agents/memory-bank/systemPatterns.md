# System patterns

## Architecture style

Single Maven module, **Spring Modulith 2.1.0** package modules (`@ApplicationModule`). Pattern aligned with [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce).

Full diagram: [docs/02-architecture.md](../docs/02-architecture.md)

## Module dependency graph

```text
mcp          → core, medicalcase, retrieval, embedding, dataset
dataset      → core, medicalcase, embedding
retrieval    → core, medicalcase, embedding
embedding    → core
medicalcase  → core
system       → core, embedding
```

## Module ownership

| Module | Package | Responsibilities | Domain models |
|---|---|---|---|
| `core` | `...core` | Config, security, exceptions, Flyway bootstrap, `@InjectSql` infra | — |
| `medicalcase` | `...medicalcase` | Domain records, `MedicalCaseRepository` API + JDBC impl | `MedicalCase`, `CaseSummary`, `SemanticMatch`, `SpecialtyCount`, `DatasetStats` |
| `embedding` | `...embedding` | `EmbeddingService`, `EmbeddingEndpointPool`, manual `OpenAiEmbeddingModel` | — (infra service) |
| `retrieval` | `...retrieval` | `VectorSearchService` — FTS, pgvector ANN, stats | Uses `medicalcase` records |
| `dataset` | `...dataset` | `DatasetLoaderService` — CSV two-pass load | — |
| `mcp` | `...mcp` | Spring AI MCP adapters | DTO mapping only |
| `system` | `...system` | Actuator health indicators | — |

## Interface / impl rule

- Public API: `{module}/service/*.java`, `{module}/repository/*.java`
- Impl: `{module}/service/impl/*`, `{module}/repository/impl/*`
- MCP and cross-module code **never** import `*.impl.*`

## Traceability map (high level)

| Requirement area | Doc section | Owning module(s) | Test layer |
|---|---|---|---|
| Dataset schema | §2, §5 | `medicalcase`, `dataset` | Flyway IT, loader IT |
| MCP tools ×5 | §6 | `mcp` → `retrieval`, `medicalcase` | MCP contract IT |
| Embeddings | §4 | `embedding`, `dataset` | Embedding IT, semantic quality |
| FTS / semantic search | §6, §8 | `retrieval` | `FtsRetrievalQualityTest`, `SemanticRetrievalQualityTest` |
| Modulith boundaries | §9 | all `package-info.java` | `ModulithArchitectureTest` |

Full requirement → module → scenario → test matrix is generated in [productContext.md](productContext.md) from `registry/*.jsonl`. Executable BDD: not yet introduced — use [bdd-traceability skill](../skills/bdd-traceability/SKILL.md) when adding `src/test/resources/features/`.

## Known gaps

- `REQ-###` formally assigned in `registry/req.jsonl`; traceability tables generated.
- M-16 CI combined quality gates pending (see `records/active/M16.md`).
- RISK-001: `docs/01-requirements.md §5` schema still shows `id UUID` — code uses 24-char hex `TEXT` (M-18).

## Modulith named interfaces

Cross-module `allowedDependencies` must reference explicit named interfaces, not `medicalcase :: *`:

- `medicalcase :: domain` — `@NamedInterface` on `medicalcase/domain`
- `medicalcase :: repository` — `@NamedInterface` on `medicalcase/repository`

Consumers: `dataset`, `retrieval` (and later `mcp`). See [DEC-008](records/decisions/DEC-008.md).