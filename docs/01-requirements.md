# Requirements

## Medical MCP Server (`medical-mcp-server`) — SRS

**Version:** 2.0.0  
**Date:** 2026-06-16  
**Author:** Siarhei Berdachuk  
**Status:** Draft  
**Doc index:** [README.md](README.md)  
**Dataset:** [hpe-ai/medical-cases-classification-tutorial](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial)

### Related documents

| # | Document | Purpose |
|---|---|---|
| | [README.md](README.md) | Documentation index and development pipeline |
| 1 | [02-architecture.md](02-architecture.md) | System diagram, Modulith modules, stack, design decisions |
| 2 | [03-design.md](03-design.md) | Schema, services, MCP class sketches |
| 3 | [04-testing.md](04-testing.md) | Test pyramid, quality gates, split usage |
| 4 | [05-deployment.md](05-deployment.md) | Config, Docker, MCP clients |
| | [use-cases.md](use-cases.md) | Actors, workflows, per-tool scenarios |
| | [future/prompt-lab.md](future/prompt-lab.md) | Optional `prompt-lab` profile — §18, M9/M10 |

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
| Spring Modulith | 2.1.0 |
| JDK | 21 |
| DB | PostgreSQL 17 + pgvector |

---

## 2. Source Dataset

**URL:** https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial

| Property | Value |
|---|---|
| Rows | 2,464 (train 1,724 · validation 370 · test 370) |
| Total size | 9.04 MB (CSV) |
| Source format | CSV (`medical_cases_{train,validation,test}.csv`) — Parquet is a HuggingFace viewer conversion only |
| Splits | `train` \| `validation` \| `test` (HuggingFace config names) |
| Classification labels | 13 `medical_specialty` values (exact strings) |

### Schema (HuggingFace — 5 columns only)

Verified via [HuggingFace Datasets API](https://datasets-server.huggingface.co/info?dataset=hpe-ai/medical-cases-classification-tutorial) (2026-06-16). The dataset has **no row id** — the server assigns a UUID on ingest.

| Column | HF type | Nullable | Description |
|---|---|---|---|
| `description` | `string` | No | Short case summary / reason for visit |
| `transcription` | `string` | No | Full clinical transcription |
| `sample_name` | `string` | No | Human-readable case title (not unique — use UUID for lookup) |
| `medical_specialty` | `string` | No | One of 13 classification labels (exact match required) |
| `keywords` | `string` | Yes | Comma-separated keywords; **~36 % of rows are null/empty** |

### `medical_specialty` values (13 — exact strings)

| Label | Rows |
|---|---|
| Cardiovascular / Pulmonary | 742 |
| Orthopedic | 408 |
| Neurology | 282 |
| Gastroenterology | 222 |
| Obstetrics / Gynecology | 182 |
| Hematology - Oncology | 120 |
| Neurosurgery | 109 |
| ENT - Otolaryngology | 80 |
| Nephrology | 71 |
| Psychiatry / Psychology | 68 |
| Ophthalmology | 66 |
| Pediatrics - Neonatal | 64 |
| Radiology | 50 |

`list_specialties` returns these labels verbatim. `specialty` filters on tools must use the **exact** label (e.g. `Cardiovascular / Pulmonary`, not `Cardiology`).

### Server-only columns (not in HuggingFace)

| Column | Purpose |
|---|---|
| `id` | UUID primary key assigned at load time |
| `split` | `train` \| `validation` \| `test` — from source file name |
| `embedding` | `VECTOR(768)` — computed at load pass 2 |
| `fts` | Generated `tsvector` for full-text search |
| `created_at` | Ingest timestamp |

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
- Use **Spring Modulith** package modules with explicit `allowedDependencies` (same approach as [`med-expert-match-ce`](https://github.com/berdachuk/med-expert-match-ce))
- Enforce **interface / implementation separation** — public contracts in `service/` and `repository/`; JDBC and framework code only in `impl/` subpackages
- Verify **retrieval response quality** on the held-out **test** split (FTS + semantic metrics — see [04-testing.md](04-testing.md))
- Maintain a **use case catalog** aligned with dataset fields ([use-cases.md](use-cases.md))

### Non-Goals

- Does not retrain or fine-tune classifiers via production MCP tools
- Does not expose a REST API — MCP is the only external contract (default profile)
- Does not manage dataset versioning or HuggingFace sync
- Does not provide a UI
- Does not expose MCP **completions** (`@McpComplete`) — incompatible with UUID-based case identity
- Does not provide a production **specialty classification API** — optional `prompt-lab` profile is future scope ([§18](#18-future-scope-optional))

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

**Pool classes** (in `embedding/`):

| Class | Role |
|---|---|
| `MultiEndpointEmbeddingProperties` | `@ConfigurationProperties`; `@NotEmpty` on `endpoints` |
| `EmbeddingEndpointPoolConfig` | Always-on `@Configuration`; builds pool `@Bean`; throws if no valid endpoints |
| `EmbeddingEndpointPool` | Worker queue, batch API calls, skip-on-failure |
| `EmbeddingService` / `EmbeddingServiceImpl` | `embedAsFloatArray` / `embedBatch` delegate to pool |

Pull on the Ollama host: `ollama pull nomic-embed-text:v1.5`

**Embedding input strategy** — embed `{sampleName}. {description} {keywords}` only; omit `keywords` segment when null/empty. `transcription` is excluded: it exceeds the 512-token context of `nomic-embed-text` and would dilute vector signal. FTS via `tsvector` covers full-text retrieval over `transcription`.

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
    split             VARCHAR(12),    -- 'train' | 'validation' | 'test'
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

All endpoints map **only** to dataset fields or server-derived search indexes (FTS, embeddings, UUID, split). No clinical inference endpoints (e.g. diagnosis extraction) — those are out of scope.

### Tools (5)

#### `search_cases`
Full-text search across `sample_name`, `description`, `transcription`, and `keywords` (via generated `fts`).

| Param | Type | Required | Description |
|---|---|---|---|
| `query` | String | Yes | Search terms |
| `specialty` | String | No | Exact `medical_specialty` label — must match one of the 13 values above |
| `split` | String | No | `train` \| `validation` \| `test` |
| `limit` | Integer | No | Max results, default 10, max 50 |

Returns: `[{ id, sampleName, description, medicalSpecialty, keywords, split }]` — `CaseSummary` (no `transcription`).

---

#### `get_case`
Fetch a single case by server-assigned UUID (from `search_cases`, `semantic_search`, or resources).

| Param | Type | Required |
|---|---|---|
| `id` | String (UUID) | Yes |

Returns: full `MedicalCase` — all five HF columns plus `id`, `split`, `createdAt`. Includes `transcription`.

---

#### `semantic_search`
Embeds the query via `nomic-embed-text:v1.5` and runs pgvector HNSW cosine similarity search.

| Param | Type | Required | Description |
|---|---|---|---|
| `query` | String | Yes | Free-text query to embed and compare |
| `specialty` | String | No | Exact `medical_specialty` pre-filter before ANN |
| `topK` | Integer | No | Number of results, default 5 |
| `minSimilarity` | Double | No | Cosine similarity threshold, default 0.70 |

Returns: `SemanticMatch` — `{ caseSummary: CaseSummary, similarity }`  
Reports progress via `McpSyncRequestContext` at 0 %, 50 % (after embedding), 100 %.

---

#### `list_specialties`
Returns all 13 `medical_specialty` values with row counts (derived from loaded data).

No params. Returns: `[{ specialty, count }]`

---

#### `get_dataset_stats`
Returns dataset-level statistics.

No params. Returns: `{ totalCases: 2464, bySpecialty: {...}, bySplit: { train: 1724, validation: 370, test: 370 } }`  
Cached 60 seconds (Caffeine).

---

### Resources (2 total)

| URI | MIME | Description |
|---|---|---|
| `medical://cases/{id}` | `application/json` | Full case record by UUID (includes transcription) |
| `medical://stats` | `application/json` | Dataset statistics snapshot |

---

### Prompts (1)

#### `case-analysis`
Injects dataset fields from a case into an LLM prompt template. **No completion handler** — obtain `caseId` from `search_cases` or `semantic_search` first (`sample_name` is not unique).

| Arg | Required | Description |
|---|---|---|
| `caseId` | Yes | Server UUID of the case |
| `focus` | No | Which dataset field(s) to emphasize: `description` \| `transcription` \| `keywords` \| `specialty` \| `all` (default) |

`focus` values map 1:1 to columns: `description`, `transcription`, `keywords`, `medical_specialty`. When `keywords` is null, the template omits that section.

**Not exposed:** MCP completions (`@McpComplete`) — removed; prefix-matching `sample_name` while the arg is named `caseId` is incompatible with the dataset model.

### MCP capabilities

| Capability | Enabled | Notes |
|---|---|---|
| tools | yes | 5 tools — all dataset-backed |
| resources | yes | 2 URIs |
| prompts | yes | 1 prompt — dataset-field focus only |
| completion | **no** | Not compatible with UUID-based case identity |

---

## 7. Data Loading

Dataset loaded **once at startup** via `CommandLineRunner`. Skipped if `SELECT COUNT(*) FROM medical_case > 0` (idempotent restart).

**Two-pass loading:**

**Pass 1 — Insert rows (no embeddings)**
1. Download CSV files from HuggingFace (`medical_cases_train.csv`, `medical_cases_validation.csv`, `medical_cases_test.csv`)
2. Parse CSV (OpenCSV, Apache Commons CSV, or HuggingFace `datasets` library)
3. Assign UUID per row; set `split` from source file; batch INSERT with `embedding = NULL`

→ Dataset is immediately queryable via FTS and specialty filter after pass 1.

**Pass 2 — Backfill embeddings**
4. For each row: build embedding text → `{sampleName}. {description} {keywords}`
5. Call `EmbeddingService` → `float[768]` via `EmbeddingEndpointPool` (`api-batch-size` 50)
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
| Modularity | `ApplicationModules.of(...).verify()` passes — no illegal cross-module references |
| Retrieval quality | FTS + semantic benchmarks on **test** split meet gates in [04-testing.md](04-testing.md) §6 |

### Domain records (`medicalcase/domain`)

| Record | Fields (summary) | Used by |
|---|---|---|
| `MedicalCase` | All HF columns + `id`, `split`, `createdAt` | `get_case`, resources, prompts |
| `CaseSummary` | `id`, `sampleName`, `description`, `medicalSpecialty`, `keywords`, `split` | `search_cases`, result bodies |
| `SemanticMatch` | `caseSummary`, `similarity` | `semantic_search` |
| `SpecialtyCount` | `specialty`, `count` | `list_specialties` |
| `DatasetStats` | `totalCases`, `bySpecialty`, `bySplit` | `get_dataset_stats`, `medical://stats` |

---

## 9. Architecture (Spring Modulith)

**Canonical reference:** [02-architecture.md](02-architecture.md) — diagram, full package tree, stack versions, design decisions.

Single deployable Spring Boot application using **package-based modules** ([`med-expert-match-ce` pattern](https://github.com/berdachuk/med-expert-match-ce)). Each module has a `package-info.java` annotated with `@ApplicationModule(allowedDependencies = …)` declaring its dependency graph. Modulith verification runs in CI (`mvn verify`).

### Package modules

```
src/main/java/com/example/medicalmcp/
├── MedicalMcpApplication.java
├── core/              # Shared config, exception, health, util — no feature deps
├── medicalcase/       # Domain records, repository API + JDBC impl
├── embedding/         # EmbeddingService API, pool, config, impl
├── retrieval/         # FTS, pgvector search, dataset stats
├── dataset/           # CSV loader (CommandLineRunner)
├── mcp/               # @McpTool / @McpResource / @McpPrompt
└── system/            # Actuator health indicators (optional)
```

### Interface / implementation rules

Mirror [`med-expert-match-ce`](https://github.com/berdachuk/med-expert-match-ce/tree/main/src/main/java/com/berdachuk/medexpertmatch):

| Layer | Public API (inject this) | Implementation |
|---|---|---|
| Repository | `{module}/repository/XxxRepository.java` | `{module}/repository/impl/XxxRepositoryImpl.java` |
| Service | `{module}/service/XxxService.java` | `{module}/service/impl/XxxServiceImpl.java` |
| MCP adapters | `mcp/*Tools.java`, `mcp/*Resources.java`, … | Delegates to service interfaces only — no JDBC in MCP layer |

- `@Repository` / `@Service` on **impl** classes only; interfaces have no Spring stereotypes
- Callers (MCP tools, other modules) depend on **interfaces**, never impl types
- SQL in `src/main/resources/sql/{module}/` (optional; same `@InjectSql` pattern as med-expert-match-ce)

### Module dependency graph

```
mcp          → core, medicalcase, retrieval, embedding, dataset
dataset      → core, medicalcase, embedding
retrieval    → core, medicalcase, embedding
embedding    → core
medicalcase  → core
system       → core, embedding
```

---

## 10. Project Layout

See [02-architecture.md — Module Structure](02-architecture.md#module-structure-spring-modulith).

---

## 11. Key Dependencies

See [02-architecture.md — Stack & Versions](02-architecture.md#stack--versions).

---

## 12. Configuration

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
        version: 1.6.0
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
          completion: false
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

## 13. Environment Variables

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

## 14. Milestones

Implementation milestones with aligned test deliverables ([04-testing.md §10](04-testing.md#10-mapping-to-milestones)).

| # | Milestone | Key deliverables | Tests | Status |
|---|---|---|---|---|
| M1 | Schema + modulith foundation | `V1__init_medical_cases.sql`, domain records, `package-info.java` per module, Boot stub | `ModulithArchitectureTest`, `FlywaySchemaIntegrationTest` | ✅ |
| M2 | Dataset loader pass 1 | `DatasetLoaderService` + impl, `MedicalCaseRepository` + impl (insert), CSV → JDBC | `DatasetLoaderIntegrationTest` (train CSV sample) | ✅ |
| M3 | Retrieval module | `MedicalCaseRepository` (FTS, findById, listSpecialties), `VectorSearchService` + impl, stats | Repository IT, `FtsRetrievalQualityTest` (subset) | ✅ |
| M4 | Embedding module | `EmbeddingService` + impl, `EmbeddingEndpointPool`, loader pass 2 | Embedding IT, `SemanticRetrievalQualityTest` | ⬜ |
| M5 | MCP module | `MedicalCaseTools` ×5, resources, `case-analysis` prompt | `McpToolsContractIntegrationTest`, `McpResourcesIntegrationTest` | ⬜ |
| M6 | Config + security | `application.yml`, `SecurityConfig`, Caffeine cache, `medicalmcp.*` properties | Config binding tests, cache TTL test | ⬜ |
| M7 | End-to-end | Claude Desktop smoke, `McpSyncClient` from med-expert-match-ce | E2E smoke checklist ([04-testing.md §11](04-testing.md#11-manual-smoke-checklist-m7)) | ⬜ |
| M8 | Docker + docs | `docker-compose.yml`, full doc set | Docker health + nightly **test** split quality gate | ⬜ |

### Optional (future)

| # | Milestone | Key deliverables | Reference |
|---|---|---|---|
| M9 | Prompt lab | `promptlab` module, meta-prompting, eval tools (`prompt-lab` profile) | [future/prompt-lab.md](future/prompt-lab.md), [§18](#18-future-scope-optional) |
| M10 | Prompt integration | Wire promoted template into `case-analysis` | [future/prompt-lab.md §9](future/prompt-lab.md#9-implementation-phases) P6 |

---

## 15. Docker Compose

```yaml
services:
  medical-mcp-server:
    build: .
    ports:
      - "8092:8092"
    environment:
      MEDICALMCP_DB_HOST: postgres
      MEDICALMCP_DB_USERNAME: medical_mcp
      MEDICALMCP_DB_PASSWORD: medical_mcp
      EMBEDDING_BASE_URL: http://host.docker.internal:11434
      EMBEDDING_API_KEY: none
      EMBEDDING_MODEL: nomic-embed-text:v1.5
      EMBEDDING_DIMENSIONS: 768
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8092/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  postgres:
    image: pgvector/pgvector:pg17
    environment:
      POSTGRES_DB: medical_mcp
      POSTGRES_USER: medical_mcp
      POSTGRES_PASSWORD: medical_mcp
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U medical_mcp"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  pgdata:
```

Ollama runs on the host (`host.docker.internal:11434`), not in Docker.

---

## 16. MCP Client Connection

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

## 17. Testing & quality assurance

Full strategy: [04-testing.md](04-testing.md). Summary:

### Test pyramid

| Layer | Profile | When |
|---|---|---|
| Unit + Modulith | default | Every `mvn test` |
| Integration (Testcontainers) | `integration` | Every PR — `mvn verify -Pintegration` |
| Response quality (test split) | `quality` | Nightly / pre-release |
| E2E MCP client | `e2e` | Manual / staging |

### Dataset splits in tests

| Split | CSV | Role |
|---|---|---|
| train (1,724) | [medical_cases_train.csv](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial/blob/main/medical_cases_train.csv) | Fixtures, fast IT |
| validation (370) | [medical_cases_validation.csv](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial/blob/main/medical_cases_validation.csv) | Threshold tuning only |
| test (370) | [medical_cases_test.csv](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial/blob/main/medical_cases_test.csv) | **Held-out quality gates** |

### Response quality metrics (production MCP)

| Tool | Key metrics | Initial test-split gates (04-testing.md) |
|---|---|---|
| `search_cases` | Hit@10, Specialty@10, MRR | sample_name Hit@10 ≥ 0.95 |
| `semantic_search` | Self@1, Self@5, Specialty@5 | Self@5 ≥ 0.65, Specialty@5 ≥ 0.85 |
| `get_case` / stats | Round-trip fidelity, exact counts | 2464 rows, 13 specialties |
| Filters | Specialty/split precision | 100 % on filtered results |

Quality CI emits `target/quality-report.json`; build fails when gates are missed.

---

## 18. Future scope (optional)

Not required for M1–M8. Documented in [future/prompt-lab.md](future/prompt-lab.md).

**`prompt-lab` Spring profile** (`medicalmcp.prompt-lab.enabled=true`):

- Optional `promptlab` Modulith module — depends on `medicalcase`, `retrieval`; not depended on by production `mcp`
- Ports [specialty-classification-reasoning](https://github.com/berdachuk/ai-architect-6-tasks/tree/main/specialty-classification-reasoning) patterns: ReAct prompts, meta-improvement with failure context, label normalization to 13 HF specialties
- MCP tools (lab only): `evaluate_specialty_prompt`, `improve_specialty_prompt`, `compare_specialty_prompts`, `gate_specialty_prompt`
- Eval on **validation** split; promote only if **test** split gate passes
- Milestones: **M9** (prompt lab), **M10** (optional `case-analysis` template wiring)

Default production server: **5 tools, 2 resources, 1 prompt, 0 completions** — unchanged.

---

## Related documentation

- [README.md](README.md) — documentation index
- [02-architecture.md](02-architecture.md) — system design, Modulith layout, stack
- [03-design.md](03-design.md) — detailed design and MCP sketches
- [04-testing.md](04-testing.md) — test strategy and quality benchmarks
- [05-deployment.md](05-deployment.md) — config, Docker, MCP clients
- [use-cases.md](use-cases.md) — actors, workflows, per-tool scenarios
- [future/prompt-lab.md](future/prompt-lab.md) — optional prompt-lab (M9/M10)
