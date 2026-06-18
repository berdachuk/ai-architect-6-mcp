# medical-mcp-server

Spring AI 2.0 MCP server that wraps the [hpe-ai/medical-cases-classification-tutorial](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial) dataset (2,464 medical cases, 13 specialties) and exposes search and retrieval tools over SSE.

**Version:** 2.0.0

## Documentation

Full index and reading order: **[docs/README.md](docs/README.md)**

| Step  | Document                                      | Contents |
|-------|-----------------------------------------------|---|
| —     | [01-requirements.md](docs/01-requirements.md) | Requirements (SRS) — source of truth |
| 1     | [02-architecture.md](docs/02-architecture.md) | Architecture (SAD) |
| 2     | [03-design.md](docs/03-design.md)             | Detailed design (SDD) |
| 3     | [04-testing.md](docs/04-testing.md)           | Test plan |
| 4     | [05-deployment.md](docs/05-deployment.md)     | Deployment & operations |
| +     | [use-cases.md](docs/use-cases.md)             | Use case catalog |
| +     | [guides/README.md](docs/guides/README.md)     | User guides — MCP, prompt-lab, LM Studio |
| +     | [AGENTS.md](AGENTS.md)                        | AI agent index — skills, memory bank, module guides |

## Dataset (verified 2026-06-16)

| Property   | Value                                                                          |
|------------|--------------------------------------------------------------------------------|
| Rows       | 2,464 — train 1,724 · validation 370 · test 370                                |
| Source     | CSV files on HuggingFace (not Parquet in repo)                                 |
| Columns    | `description`, `transcription`, `sample_name`, `medical_specialty`, `keywords` |
| `keywords` | Nullable (~36 % empty)                                                         |
| `id`       | Server-generated UUID (not in HuggingFace)                                     |

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

| Kind        | Count  | Notes                                                 |
|-------------|--------|-------------------------------------------------------|
| Tools       | 5      | FTS, UUID lookup, semantic search, specialties, stats |
| Resources   | 2      | Case by UUID, stats snapshot                          |
| Prompts     | 1      | `case-analysis` — focus on dataset fields only        |
| Completions | **0**  | Removed — incompatible with UUID identity             |

## Architecture

Single Maven module; boundaries enforced by Spring Modulith `@ApplicationModule` on each package:

| Module        | Responsibility                                                               |
|---------------|------------------------------------------------------------------------------|
| `core`        | Shared config, security, exceptions, utilities                               |
| `medicalcase` | Domain records, `MedicalCaseRepository` API + JDBC impl                      |
| `embedding`   | `EmbeddingService` API, `EmbeddingEndpointPool`, config                      |
| `retrieval`   | `VectorSearchService` API — FTS helpers, pgvector search, stats              |
| `dataset`     | `DatasetLoaderService` — CSV → JDBC two-pass load                            |
| `mcp`         | `@McpTool` / `@McpResource` / `@McpPrompt` — injects service interfaces only |
| `system`      | Actuator health indicators (optional)                                        |

Details: [docs/02-architecture.md](docs/02-architecture.md)

Modulith verification: `mvn clean verify`

## Prerequisites

- JDK 21, Maven 3.9+
- PostgreSQL 17 with pgvector (or Docker via **WSL** on Windows)
- Ollama with `nomic-embed-text:v1.5` pulled

## Quick start

### Docker Compose (recommended)

Requires Ollama on the host with `nomic-embed-text:v1.5` (`ollama pull nomic-embed-text:v1.5`).

```bash
docker compose up --build
```

MCP endpoint: `http://localhost:8092/sse`  
Health: `http://localhost:8092/actuator/health`

First startup streams the HuggingFace CSVs (see `medicalmcp.dataset.loader.sources` in `application.yml`) straight into Postgres and embeds all 2,464 rows. No files are written to disk. Takes several minutes.

### Local development (IDE — IntelliJ IDEA)

Run the database with the dev compose file, start the embedding model on the host, then launch the app from the IDE.

```bash
docker compose -f docker-compose.dev.yml up -d          # Postgres + pgvector on :5436
ollama serve &                                           # if not already running
ollama pull nomic-embed-text:v1.5                        # one-time
```

**IntelliJ IDEA run configuration** (Spring Boot → `MedicalMcpServerApplication`):

| Field                 | Value                                                              |
|-----------------------|--------------------------------------------------------------------|
| Active profiles       | `dev`                                                              |
| Environment variables | `MEDICALMCP_DATASET_LOADER_ENABLED=true`                           |
| VM options (optional) | `-Dspring-boot.run.fork=false`                                     |

Then click **Run**. The `dev` profile (see `application-dev.yml`) points `MEDICALMCP_DB_HOST` at `localhost`, so the IDE process connects to the containerised Postgres.

Add `debug` to **Active profiles** (e.g. `dev,debug`) for verbose internal logging: per-package DEBUG on every `com.example.medicalmcp.*` module, TRACE on the embedding pool worker loop, DEBUG on `JdbcTemplate` (SQL statements + bind parameters), and `management.endpoint.health.show-details: always` so `/actuator/health` exposes the per-endpoint LLM probe details. Useful for tracing MCP request flows end-to-end; expect ~10× more console output.

**Dataset ingest** — first run with `MEDICALMCP_DATASET_LOADER_ENABLED=true` streams the three HuggingFace CSVs (train / validation / test, configured in `medicalmcp.dataset.loader.sources`) straight into Postgres via `DatasetLoaderService` (no files written to disk), then embeds all 2,464 rows through the local Ollama endpoint. Idempotent — if `COUNT(*) > 0` the pass is skipped.

To trigger a clean reload (e.g. after pulling new upstream data), stop the app and reset the volume:

```bash
docker compose -f docker-compose.dev.yml down -v         # wipes pgdata-dev
docker compose -f docker-compose.dev.yml up -d
```

> **Windows:** run Docker / Ollama from **WSL** and start IntelliJ from the same WSL distro so `localhost` resolves to the WSL network namespace — see [.agents/memory-bank/techContext.md](.agents/memory-bank/techContext.md).

```bash
mvn clean verify -Pintegration   # WSL on Windows (Testcontainers)
```

### Where the database lives

Postgres data lives in a Docker **named volume** managed by Compose — never inside the repo.

| Compose file              | Volume         | Container mount                  |
|---------------------------|----------------|---------------------------------|
| `docker-compose.yml`      | `pgdata`       | `/var/lib/postgresql/data`      |
| `docker-compose.dev.yml`  | `pgdata-dev`   | `/var/lib/postgresql/data`      |

Inspect / remove:

```bash
docker volume ls | grep pgdata
docker volume inspect medical-mcp-server_pgdata-dev
docker compose -f docker-compose.dev.yml down -v   # also drops pgdata-dev
```

Flyway migrations are applied automatically from `src/main/resources/db/migration` on startup; the dataset (2,464 rows + embeddings) is written into the same volume by `DatasetLoaderService`.

> **WSL / Windows:** named volumes are stored under Docker Desktop's data root (e.g. `\\wsl$\docker-desktop-data\…`), not on the Windows filesystem.

### Maven profiles

| Profile                      | Command                    |
|------------------------------|----------------------------|
| Unit + Modulith              | `mvn test`                 |
| Integration (Testcontainers) | `mvn verify -Pintegration` |
| E2E smoke (SSE client)       | `mvn verify -Pe2e`         |
| Quality gate (test split)    | `mvn verify -Pquality`     |
| Prompt lab (offline eval)    | `mvn verify -Pprompt-lab`  |

### Spring profiles

| Profile             | File / source               | What it does                                                                                  |
|---------------------|-----------------------------|-----------------------------------------------------------------------------------------------|
| `default`           | `application.yml`           | Production defaults — INFO logs, `db` health indicator on.                                    |
| `dev`               | `application-dev.yml`       | `MEDICALMCP_DB_HOST=localhost`, dataset loader on, embedding endpoint at `localhost:11434`.   |
| `debug`             | `application-debug.yml`     | DEBUG on every `com.example.medicalmcp.*` module, TRACE on the embedding pool worker, DEBUG on `JdbcTemplate` (SQL + bind params), `management.endpoint.health.show-details: always`. Combine with `dev`: `dev,debug`. |

Full deployment guide: [docs/05-deployment.md](docs/05-deployment.md)

## Related projects

- [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce) — GraphRAG consumer; shares embedding model, pool pattern, and Modulith layout
