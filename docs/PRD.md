# Product Requirements Document
## Medical MCP Server (`medical-mcp-server`)

**Version:** 1.3.0  
**Date:** 2026-06-16  
**Author:** Siarhei Berdachuk  
**Status:** Draft  
**Implementation plan:** [PLAN.md](PLAN.md)  
**Dataset:** [hpe-ai/medical-cases-classification-tutorial](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial)

---

## 1. Overview

`medical-mcp-server` is a **Spring AI 2.0.0 MCP server** that wraps the `hpe-ai/medical-cases-classification-tutorial` HuggingFace dataset and exposes it to AI agents and LLM clients over SSE transport.

The server loads the dataset (2,464 rows, ~9 MB) into a PostgreSQL + pgvector database and exposes structured lookup, full-text search, and semantic similarity search via the MCP protocol.

**Runtime identity**

| Property | Value |
|---|---|
| Port | `8092` |
| Transport | Spring MVC SSE (SYNC) |
| Spring Boot | 4.1.0 |
| Spring AI | 2.0.0 |
| JDK | 21 |
| DB | PostgreSQL 17 + pgvector |

---

## 2. Source Dataset

**URL:** https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial

| Property | Value |
|---|---|
| Rows | 2,464 |
| Raw size | 9.04 MB |
| Parquet size | 4.29 MB |
| Splits | Pre-split (train / test / validation) |
| Classification labels | 13 medical specialties |

### Schema

| Column | Type | Description |
|---|---|---|
| `description` | `text` | Short case summary / reason for visit |
| `transcription` | `text` | Full clinical transcription (procedure notes, HPI, findings, labs, plan) |
| `sample_name` | `text` | Human-readable case title (e.g. "Pacemaker Interrogation") |
| `medical_specialty` | `text` | Classification label — 13 distinct values |
| `keywords` | `text` | Comma-separated clinical keywords |

### Known specialties (13 classes)

Cardiovascular / Pulmonary, Orthopedic, Nephrology, Obstetrics / Gynecology, and 9 others present in the dataset.

---

## 3. Goals & Non-Goals

### Goals

- Load and persist the HuggingFace dataset into PostgreSQL via Flyway-managed schema
- Expose **search and retrieval** of medical cases as named MCP tools
- Support **semantic similarity search** using pgvector embeddings aligned with `med-expert-match-ce` GraphRAG (same model, same dimensions)
- Route **all embeddings** through a mandatory **`EmbeddingEndpointPool`** — at least one endpoint must be configured; application fails fast at startup if `endpoints` is empty
- Support **full-text search** using `tsvector` on `sample_name`, `description`, `transcription`, and `keywords`
- Support **specialty-based filtering** (13 classification labels)
- Be consumable by Claude Desktop and `McpSyncClient` inside `med-expert-match-ce`

### Non-Goals

- Does not retrain or fine-tune classifiers
- Does not expose a REST API — MCP is the only external contract
- Does not manage dataset versioning or HuggingFace sync
- Does not provide a UI

---

## 4. Embedding Model

Defaults align with [`med-expert-match-ce`](https://github.com/berdachuk/med-expert-match-ce) GraphRAG embeddings: **Ollama** serving **`nomic-embed-text:v1.5`** at **768 dimensions**. Use Ollama model tags (not LM Studio display names). Do not mix embedding models with different dimensions in the same database.

**No Spring AI embedding auto-configuration.** Exclude `OpenAiEmbeddingAutoConfiguration` (same as `med-expert-match-ce`). Build `OpenAiEmbeddingModel` instances manually and route **all** embedding traffic through **`EmbeddingEndpointPool`** — always on, never optional. At least one endpoint with a non-blank `url` is required; startup fails if the pool would be empty.

Shared credentials and dimensions (env → `spring.ai.custom.embedding.*`):

```yaml
spring:
  ai:
    openai:
      enabled: false
    custom:
      embedding:
        api-key: ${EMBEDDING_API_KEY:none}
        dimensions: ${EMBEDDING_DIMENSIONS:768}
```

Pool configuration (`medicalmcp.embedding.multi-endpoint` — **required**; minimum one endpoint):

```yaml
medicalmcp:
  embedding:
    multi-endpoint:
      endpoints:                       # @NotEmpty — at least one entry required
        - url: ${EMBEDDING_BASE_URL:http://localhost:11434}
          model: ${EMBEDDING_MODEL:nomic-embed-text:v1.5}
          priority: 1
        # Additional nodes (optional; same model + dimensions):
        # - url: http://192.168.0.73:11434
        #   model: nomic-embed-text:v1.5
        #   priority: 2
        #   workers: 2
      skip-duration-min: ${MEDICALMCP_EMBEDDING_MULTI_ENDPOINT_SKIP_MIN:10}
      worker-per-endpoint: ${MEDICALMCP_EMBEDDING_MULTI_ENDPOINT_WORKERS:1}
      api-batch-size: ${MEDICALMCP_EMBEDDING_MULTI_ENDPOINT_API_BATCH_SIZE:50}
```

`EmbeddingEndpointPoolConfig` constructs one `OpenAiEmbeddingModel` per endpoint (OpenAI-compatible / Ollama). URL normalization appends `/v1` when building each model — same helper as `med-expert-match-ce`. Additional Ollama nodes use the same model and dimensions.

**Pool classes** (in `medical-mcp-infrastructure/embedding/`):

| Class | Role |
|---|---|
| `MultiEndpointEmbeddingProperties` | `@ConfigurationProperties`; `@NotEmpty` on `endpoints` |
| `EmbeddingEndpointPoolConfig` | Always-on `@Configuration`; builds pool `@Bean`; throws if no valid endpoints |
| `EmbeddingEndpointPool` | Worker queue, batch API calls, skip-on-failure |
| `MedicalEmbeddingService` | `embed` / `batchEmbed` delegate to pool |

Pull on the Ollama host: `ollama pull nomic-embed-text:v1.5`

**Embedding input strategy** — embed `{sampleName}. {description} {keywords}` only. `transcription` is excluded: it exceeds the 512-token context of `nomic-embed-text` and would dilute vector signal. FTS via `tsvector` covers full-text retrieval over `transcription`.

---

## 5. Database Schema

Single table mapping directly to the dataset columns, plus derived columns for search.

```sql
CREATE TABLE medical_case (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    sample_name       TEXT        NOT NULL,
    description       TEXT,
    transcription     TEXT,
    medical_specialty VARCHAR(120),
    keywords          TEXT,
    split             VARCHAR(12),    -- 'train' | 'test' | 'validation'
    embedding         VECTOR(768),    -- nomic-embed-text:v1.5 @ 768 dims
    fts               TSVECTOR GENERATED ALWAYS AS (
                          to_tsvector('english',
                              coalesce(sample_name,   '') || ' ' ||
                              coalesce(description,   '') || ' ' ||
                              coalesce(transcription, '') || ' ' ||
                              coalesce(keywords,      ''))
                      ) STORED,
    created_at        TIMESTAMPTZ DEFAULT now()
);

-- ANN index: HNSW cosine (normalised output of nomic-embed-text)
-- At 2,464 rows HNSW builds in < 1 s; no IVFFlat tuning needed
CREATE INDEX idx_medical_case_embedding
    ON medical_case USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Full-text index
CREATE INDEX idx_medical_case_fts
    ON medical_case USING GIN (fts);

-- Specialty filter (pre-filter before ANN)
CREATE INDEX idx_medical_case_specialty
    ON medical_case (medical_specialty);

-- Trigram index for ILIKE specialty search
CREATE INDEX idx_medical_case_specialty_trgm
    ON medical_case USING GIN (medical_specialty gin_trgm_ops);
```

---

## 6. MCP Surface

### Tools (5 total)

#### `search_cases`
Full-text search across `sample_name`, `description`, `transcription`, and `keywords`.

| Param | Type | Required | Description |
|---|---|---|---|
| `query` | String | Yes | Search terms |
| `specialty` | String | No | Filter by medical specialty (exact match) |
| `split` | String | No | Filter by dataset split: `train` \| `test` \| `validation` |
| `limit` | Integer | No | Max results, default 10, max 50 |

Returns: `[{ id, sampleName, description, medicalSpecialty, keywords, split }]`

---

#### `get_case`
Fetch a single case by UUID, including the full transcription.

| Param | Type | Required |
|---|---|---|
| `id` | String (UUID) | Yes |

Returns: full `MedicalCase` record including `transcription`.

---

#### `semantic_search`
Embeds the query via `nomic-embed-text:v1.5` and runs pgvector HNSW cosine similarity search.

| Param | Type | Required | Description |
|---|---|---|---|
| `query` | String | Yes | Free-text query to embed and compare |
| `specialty` | String | No | Pre-filter by specialty before ANN |
| `topK` | Integer | No | Number of results, default 5 |
| `minSimilarity` | Double | No | Cosine similarity threshold, default 0.70 |

Returns: `SemanticMatch` — `{ case: CaseSummary, similarity }`  
Reports progress via `McpSyncRequestContext` at 0 %, 50 % (after embedding), 100 %.

---

#### `list_specialties`
Returns all 13 distinct specialty values with case counts.

No params. Returns: `[{ specialty, count }]`

---

#### `get_dataset_stats`
Returns dataset-level statistics.

No params. Returns: `{ totalCases, bySpecialty: {...}, bySplit: { train, test, validation } }`  
Cached 60 seconds (Caffeine).

---

### Resources (2 total)

| URI | MIME | Description |
|---|---|---|
| `medical://cases/{id}` | `application/json` | Full case record by UUID (includes transcription) |
| `medical://stats` | `application/json` | Dataset statistics snapshot |

---

### Prompts (1 total)

#### `case-analysis`
Prompt template for LLM-driven analysis of a retrieved case. Fetches the full `MedicalCase` from DB and injects it into the template.

| Arg | Required | Description |
|---|---|---|
| `caseId` | Yes | UUID of the case to analyse |
| `focus` | No | Analysis focus: `diagnosis` \| `treatment` \| `keywords` \| `summary` |

---

### Completion (1 total)

`@McpComplete(prompt="case-analysis")` on `caseId` — prefix-matches `sample_name` from DB and returns UUID hints.

---

## 7. Data Loading

Dataset loaded **once at startup** via `CommandLineRunner`. Skipped if `SELECT COUNT(*) FROM medical_case > 0` (idempotent restart).

**Two-pass loading:**

**Pass 1 — Insert rows (no embeddings)**
1. Download Parquet files from HuggingFace CDN (train / test / validation splits)
2. Parse Parquet (DuckDB JDBC or Apache Arrow)
3. Batch INSERT into `medical_case` with `embedding = NULL`

→ Dataset is immediately queryable via FTS and specialty filter after pass 1.

**Pass 2 — Backfill embeddings**
4. For each row: build embedding text → `{sampleName}. {description} {keywords}`
5. Call `MedicalEmbeddingService` → `float[768]` via `EmbeddingEndpointPool` (`api-batch-size` 50)
6. Batch UPDATE `embedding` column — 50 rows per batch (matches `med-expert-match-ce` api-batch-size)
7. Log progress every 100 rows

Loader is skippable:

```yaml
medicalmcp:
  dataset:
    loader:
      enabled: ${MEDICALMCP_DATASET_LOADER_ENABLED:true}
      batch-size: ${MEDICALMCP_DATASET_LOADER_BATCH_SIZE:50}
```

---

## 8. Non-Functional Requirements

| Metric | Target |
|---|---|
| `search_cases` p99 | < 100 ms |
| `semantic_search` p99 (excl. embedding latency) | < 400 ms |
| `get_case` p99 | < 30 ms |
| `get_dataset_stats` p99 | < 20 ms (cached) |
| Startup (embeddings already stored) | < 10 s |
| Full initial load incl. embedding generation | Depends on Ollama throughput |
| JDBC only | No JPA/Hibernate — `NamedParameterJdbcTemplate` throughout |
| Embedding pool | `medicalmcp.embedding.multi-endpoint.endpoints` must contain ≥1 valid URL; fail fast at startup |

---

## 9. Project Structure

```
medical-mcp-server/
├── pom.xml                          # Parent BOM aggregator
├── medical-mcp-domain/              # Java 21 records, zero framework deps
├── medical-mcp-infrastructure/      # JDBC repos, pgvector, Flyway, dataset loader, embedding pool
│   └── embedding/                   # EmbeddingEndpointPool + config (port from med-expert-match-ce)
├── medical-mcp-tools/               # @McpTool / @McpResource / @McpPrompt beans
└── medical-mcp-app/                 # Spring Boot entry point, security, application.yml
```

---

## 10. Key Dependencies

| Dependency | Version | Notes |
|---|---|---|
| Spring Boot | 4.1.0 | Aligned with `med-expert-match-ce` |
| Spring AI BOM | 2.0.0 | |
| `spring-ai-starter-mcp-server-webmvc` | 2.0.0 | SSE transport |
| `spring-ai-openai` | 2.0.0 | Manual `OpenAiEmbeddingModel` in embedding pool (no auto-config) |
| `postgresql` | 42.7.11 | Aligned with `med-expert-match-ce` |
| `pgvector` | 0.1.6 | |
| `flyway-database-postgresql` | 10.x | |
| `caffeine` | 3.2.4 | Stats cache |
| `commons-lang3` | 3.20.0 | |
| `spring-boot-starter-jdbc` | (Boot BOM) | NamedParameterJdbcTemplate |
| `spring-boot-starter-actuator` | (Boot BOM) | |
| Testcontainers | 2.0.5 | `pgvector/pgvector:pg17` |

---

## 11. Configuration

```yaml
spring:
  application:
    name: medical-mcp-server
  autoconfigure:
    exclude:
      - org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration
  datasource:
    url: jdbc:postgresql://${MEDICALMCP_DB_HOST:localhost}:5432/${MEDICALMCP_DB_NAME:medical_mcp}
    username: ${MEDICALMCP_DB_USERNAME:medical_mcp}
    password: ${MEDICALMCP_DB_PASSWORD:medical_mcp}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 3
      connection-init-sql: SET hnsw.ef_search = 40
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  ai:
    openai:
      enabled: false
    custom:
      embedding:
        api-key: ${EMBEDDING_API_KEY:none}
        dimensions: ${EMBEDDING_DIMENSIONS:768}
    mcp:
      server:
        name: medical-mcp-server
        version: 1.3.0
        type: SYNC
        instructions: >
          Wraps hpe-ai/medical-cases-classification-tutorial (2,464 rows, 13 specialties).
          Use search_cases for keyword/FTS queries; semantic_search for similarity;
          get_case to retrieve a full transcription by UUID; list_specialties to explore labels.
          Embeddings: nomic-embed-text:v1.5 @ 768 dims — same model as med-expert-match-ce GraphRAG.
        sse-endpoint: /sse
        sse-message-endpoint: /mcp/message
        keep-alive-interval: 30s
        capabilities:
          tool: true
          resource: true
          prompt: true
          completion: true
        annotation-scanner:
          enabled: true

medicalmcp:
  embedding:
    multi-endpoint:
      endpoints:
        - url: ${EMBEDDING_BASE_URL:http://localhost:11434}
          model: ${EMBEDDING_MODEL:nomic-embed-text:v1.5}
          priority: 1
      skip-duration-min: ${MEDICALMCP_EMBEDDING_MULTI_ENDPOINT_SKIP_MIN:10}
      worker-per-endpoint: ${MEDICALMCP_EMBEDDING_MULTI_ENDPOINT_WORKERS:1}
      api-batch-size: ${MEDICALMCP_EMBEDDING_MULTI_ENDPOINT_API_BATCH_SIZE:50}
  dataset:
    loader:
      enabled: ${MEDICALMCP_DATASET_LOADER_ENABLED:true}
      batch-size: ${MEDICALMCP_DATASET_LOADER_BATCH_SIZE:50}
  retrieval:
    similarity-threshold: ${MEDICALMCP_RETRIEVAL_SIMILARITY_THRESHOLD:0.70}
    default-top-k: ${MEDICALMCP_RETRIEVAL_DEFAULT_TOP_K:5}
    max-limit: ${MEDICALMCP_RETRIEVAL_MAX_LIMIT:50}
  cache:
    stats-ttl-seconds: ${MEDICALMCP_CACHE_STATS_TTL:60}

server:
  port: ${SERVER_PORT:8092}
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
```

---

## 12. Environment Variables

Same naming convention as `med-expert-match-ce`:

| Variable | Default | Description |
|---|---|---|
| `MEDICALMCP_DB_HOST` | `localhost` | PostgreSQL host |
| `MEDICALMCP_DB_NAME` | `medical_mcp` | Database name |
| `MEDICALMCP_DB_USERNAME` | `medical_mcp` | DB user |
| `MEDICALMCP_DB_PASSWORD` | `medical_mcp` | DB password |
| `EMBEDDING_BASE_URL` | `http://localhost:11434` | **Required.** Ollama host for pool `endpoints[0]` (`/v1` appended at model build time) |
| `EMBEDDING_API_KEY` | `none` | Ollama does not require a key |
| `EMBEDDING_MODEL` | `nomic-embed-text:v1.5` | Must match DB vector dimension |
| `EMBEDDING_DIMENSIONS` | `768` | Must not change after first load |
| `MEDICALMCP_EMBEDDING_MULTI_ENDPOINT_SKIP_MIN` | `10` | Minutes to skip a failed pool endpoint |
| `MEDICALMCP_EMBEDDING_MULTI_ENDPOINT_WORKERS` | `1` | Worker threads per endpoint |
| `MEDICALMCP_EMBEDDING_MULTI_ENDPOINT_API_BATCH_SIZE` | `50` | Texts per OpenAI-compatible embed API call |
| `MEDICALMCP_DATASET_LOADER_ENABLED` | `true` | Set to `false` after initial load |
| `MEDICALMCP_DATASET_LOADER_BATCH_SIZE` | `50` | JDBC insert batch size (pass 1) |
| `MEDICALMCP_RETRIEVAL_SIMILARITY_THRESHOLD` | `0.70` | Default `minSimilarity` for `semantic_search` |
| `MEDICALMCP_RETRIEVAL_DEFAULT_TOP_K` | `5` | Default `topK` for `semantic_search` |
| `MEDICALMCP_RETRIEVAL_MAX_LIMIT` | `50` | Max `limit` for `search_cases` |
| `MEDICALMCP_CACHE_STATS_TTL` | `60` | Stats cache TTL (seconds) |
| `SERVER_PORT` | `8092` | MCP server port |

---

## 13. Milestones

| # | Milestone | Key deliverables | Status |
|---|---|---|---|
| M1 | Schema + domain | `V1__init_medical_cases.sql`, domain records (`MedicalCase`, `CaseSummary`, `SemanticMatch`, `SpecialtyCount`, `DatasetStats`), Boot stub | ⬜ |
| M2 | Dataset loader pass 1 | `DatasetLoader` (Parquet → JDBC batch insert, `embedding = NULL`), `MedicalCaseRepository` inserts | ⬜ |
| M3 | Repositories | `MedicalCaseRepository` (FTS, findById, listSpecialties), `PgVectorSearchService` (cosine), stats query | ⬜ |
| M4 | Embedding wiring | `EmbeddingEndpointPool`, `EmbeddingEndpointPoolConfig`, `MedicalEmbeddingService`, loader pass 2, Testcontainers IT | ⬜ |
| M5 | MCP surface | `MedicalCaseTools` ×5, `MedicalCaseResources` ×2, `MedicalCasePrompts`, `MedicalCaseCompletions` | ⬜ |
| M6 | Config + security | `application.yml`, `SecurityConfig`, Caffeine stats cache, `medicalmcp.*` properties | ⬜ |
| M7 | End-to-end | Claude Desktop smoke test, `McpSyncClient` call from `med-expert-match-ce` | ⬜ |
| M8 | Docker + docs | `docker-compose.yml`, `README.md`, startup guide | ⬜ |

---

## 14. Docker Compose

```yaml
services:
  medical-mcp-server:
    build: ./medical-mcp-app
    ports:
      - "8092:8092"
    environment:
      MEDICALMCP_DB_HOST: postgres
      EMBEDDING_BASE_URL: http://host.docker.internal:11434
      EMBEDDING_MODEL: nomic-embed-text:v1.5
      EMBEDDING_DIMENSIONS: 768
    depends_on:
      postgres:
        condition: service_healthy

  postgres:
    image: pgvector/pgvector:pg17
    environment:
      POSTGRES_DB: medical_mcp
      POSTGRES_USER: medical_mcp
      POSTGRES_PASSWORD: medical_mcp
```

Ollama runs on the host (`host.docker.internal:11434`), not in Docker.

---

## 15. MCP Client Connection

**Claude Desktop** — `~/.config/claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "medical-mcp-server": {
      "url": "http://localhost:8092/sse",
      "transport": "sse"
    }
  }
}
```

**med-expert-match-ce** — `spring.ai.mcp.client.sse.connections.medical-dataset.url: http://localhost:8092/sse`

---

## Related documentation

- [PLAN.md](PLAN.md) — architecture, class-level design, implementation milestones
