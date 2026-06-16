# medical-mcp-server

Spring AI 2.0 MCP server that wraps the [hpe-ai/medical-cases-classification-tutorial](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial) dataset (2,464 medical cases, 13 specialties) and exposes search and retrieval tools over SSE.

**Version:** 2.0.0

## Documentation

Full index and reading order: **[docs/README.md](docs/README.md)**

| Step | Document | Contents |
|---|---|---|
| — | [01-requirements.md](docs/01-requirements.md) | Requirements (SRS) — source of truth |
| 1 | [02-architecture.md](docs/02-architecture.md) | Architecture (SAD) |
| 2 | [03-design.md](docs/03-design.md) | Detailed design (SDD) |
| 3 | [04-testing.md](docs/04-testing.md) | Test plan |
| 4 | [05-deployment.md](docs/05-deployment.md) | Deployment & operations |
| + | [use-cases.md](docs/use-cases.md) | Use case catalog |
| + | [AGENTS.md](AGENTS.md) | AI agent index — skills, memory bank, module guides |

## Dataset (verified 2026-06-16)

| Property | Value |
|---|---|
| Rows | 2,464 — train 1,724 · validation 370 · test 370 |
| Source | CSV files on HuggingFace (not Parquet in repo) |
| Columns | `description`, `transcription`, `sample_name`, `medical_specialty`, `keywords` |
| `keywords` | Nullable (~36 % empty) |
| `id` | Server-generated UUID (not in HuggingFace) |

Full specialty list and API mapping: [docs/01-requirements.md §2](docs/01-requirements.md#2-source-dataset)

## Stack

- Java 21, Spring Boot 4.1.0, Spring AI 2.0.0, **Spring Modulith 2.1.0**
- Single-module layout with package modules — same pattern as [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce)
- PostgreSQL 17 + pgvector (HNSW cosine, FTS)
- Embeddings: `nomic-embed-text:v1.5` @ 768 dims via Ollama (OpenAI-compatible client)
- **Embedding transport:** mandatory `EmbeddingEndpointPool` — at least one endpoint required at startup
- JDBC only (`NamedParameterJdbcTemplate`) — no JPA
- MCP port: `8092` (`/sse`)

## MCP surface (dataset-compatible only)

| Kind | Count | Notes |
|---|---|---|
| Tools | 5 | FTS, UUID lookup, semantic search, specialties, stats |
| Resources | 2 | Case by UUID, stats snapshot |
| Prompts | 1 | `case-analysis` — focus on dataset fields only |
| Completions | **0** | Removed — incompatible with UUID identity |

## Architecture

Single Maven module; boundaries enforced by Spring Modulith `@ApplicationModule` on each package:

| Module | Responsibility |
|---|---|
| `core` | Shared config, security, exceptions, utilities |
| `medicalcase` | Domain records, `MedicalCaseRepository` API + JDBC impl |
| `embedding` | `EmbeddingService` API, `EmbeddingEndpointPool`, config |
| `retrieval` | `VectorSearchService` API — FTS helpers, pgvector search, stats |
| `dataset` | `DatasetLoaderService` — CSV → JDBC two-pass load |
| `mcp` | `@McpTool` / `@McpResource` / `@McpPrompt` — injects service interfaces only |
| `system` | Actuator health indicators (optional) |

Details: [docs/02-architecture.md](docs/02-architecture.md)

Modulith verification: `mvn clean verify`

## Prerequisites

- JDK 21, Maven 3.9+
- PostgreSQL 17 with pgvector
- Ollama with `nomic-embed-text:v1.5` pulled

## Quick start (planned)

```bash
docker run -d --name medical-mcp-pg \
  -e POSTGRES_DB=medical_mcp \
  -e POSTGRES_USER=medical_mcp \
  -e POSTGRES_PASSWORD=medical_mcp \
  -p 5432:5432 pgvector/pgvector:pg17

ollama pull nomic-embed-text:v1.5

mvn clean verify
mvn spring-boot:run
```

MCP endpoint: `http://localhost:8092/sse`

Full deployment guide: [docs/05-deployment.md](docs/05-deployment.md)

## Related projects

- [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce) — GraphRAG consumer; shares embedding model, pool pattern, and Modulith layout
