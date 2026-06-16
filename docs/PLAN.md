# Architecture & Implementation Plan
## `medical-mcp-server` — HuggingFace Dataset Wrapper

**Version:** 1.3.0  
**Date:** 2026-06-16  
**Requirements:** [PRD.md](PRD.md)  
**Dataset:** [hpe-ai/medical-cases-classification-tutorial](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial)  
**Embedding alignment:** [`med-expert-match-ce`](https://github.com/berdachuk/med-expert-match-ce) — `nomic-embed-text:v1.5` @ 768 dims, `EmbeddingEndpointPool`

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│  MCP Clients                                                    │
│  ┌──────────────────┐      ┌──────────────────────────────────┐ │
│  │  Claude Desktop  │      │  med-expert-match-ce             │ │
│  │  (SSE config)    │      │  McpSyncClient → :8092/sse       │ │
│  └────────┬─────────┘      └──────────────┬───────────────────┘ │
└───────────┼────────────────────────────────┼────────────────────┘
            │  SSE / MCP 2024-11-05          │
            ▼                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  medical-mcp-server  :8092                                      │
│                                                                 │
│  medical-mcp-tools (Spring AI annotation layer)                 │
│  ┌──────────────────┬───────────────────┬──────────────────┐   │
│  │ @McpTool ×5      │ @McpResource ×2   │ @McpPrompt ×1    │   │
│  └────────┬─────────┴─────────┬─────────┴──────────────────┘   │
│           │                   │                                 │
│  medical-mcp-infrastructure (JDBC — no JPA)                     │
│  ┌──────────────────┬───────────────────┬──────────────────┐   │
│  │ MedicalCase      │ PgVector          │ Dataset          │   │
│  │ Repository       │ SearchService     │ Loader           │   │
│  │ (NamedJdbcTmpl)  │ (FTS + vector)    │ (Parquet→JDBC)   │   │
│  │                  │ EmbeddingEndpoint │                  │   │
│  │                  │ Pool (multi-node) │                  │   │
│  └────────┬─────────┴─────────┬─────────┴──────┬───────────┘   │
│           │                   │                 │               │
│  medical-mcp-app (Boot entry, Security, Actuator)               │
└───────────┼───────────────────┼─────────────────┼───────────────┘
            │                   │                 │
            ▼                   ▼                 ▼
┌────────────────────┐  ┌────────────────┐  ┌──────────────────┐
│  PostgreSQL 17     │  │  Ollama        │  │  HuggingFace     │
│  + pgvector ext.   │  │  nomic-embed   │  │  Parquet files   │
│  medical_case tbl  │  │  text:v1.5     │  │  (one-time load) │
│  HNSW idx 768-dim  │  │  768 dims      │  │                  │
└────────────────────┘  └────────────────┘  └──────────────────┘
```

---

## Module Structure

```
medical-mcp-server/
├── pom.xml                                      # Parent BOM
├── medical-mcp-domain/                          # Pure Java 21 records
├── medical-mcp-infrastructure/                  # JDBC, pgvector, loader, Flyway, embedding pool
│   └── embedding/                               # EmbeddingEndpointPool + config (port from med-expert-match-ce)
├── medical-mcp-tools/                           # @McpTool / @McpResource / @McpPrompt
└── medical-mcp-app/                             # Boot entry, security, application.yml
```

### Module dependency direction

```
medical-mcp-app
  └── medical-mcp-tools
        └── medical-mcp-infrastructure
              └── medical-mcp-domain (zero deps)
```

---

## Embedding Model

Defaults align with [`med-expert-match-ce`](https://github.com/berdachuk/med-expert-match-ce) GraphRAG embeddings: **Ollama** serving **`nomic-embed-text:v1.5`** at **768 dimensions**. Use Ollama model tags (not LM Studio display names). Do not mix embedding models with different dimensions in the same database.

**No Spring AI embedding auto-configuration** — mirror `med-expert-match-ce`: exclude `OpenAiEmbeddingAutoConfiguration`, set `spring.ai.openai.enabled: false`, and wire embeddings manually via **`EmbeddingEndpointPool`**. The pool is **always used** for every embed call (loader pass 2, `semantic_search`). At least one endpoint must be configured; startup fails if `endpoints` is empty.

Shared embedding credentials (`EMBEDDING_*` env → `spring.ai.custom.embedding.*`):

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

Pool (`medicalmcp.embedding.multi-endpoint` — **required**; minimum one endpoint):

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

`EmbeddingEndpointPoolConfig` builds one `OpenAiEmbeddingModel` per endpoint using `OpenAiEmbeddingOptions.builder().baseUrl(...).model(...).dimensions(...)`. `normalizeOpenAiBaseUrl()` appends `/v1` when constructing each model — same helper as `med-expert-match-ce`.

Environment variables (same names as `med-expert-match-ce`):

```bash
EMBEDDING_BASE_URL=http://localhost:11434
EMBEDDING_API_KEY=none
EMBEDDING_MODEL=nomic-embed-text:v1.5
EMBEDDING_DIMENSIONS=768
```

Pull on the Ollama host: `ollama pull nomic-embed-text:v1.5`

---

## Stack & Versions

Aligned exactly with `med-expert-match-ce` versions to avoid dep-hell:

| Dependency | Version | Notes |
|---|---|---|
| Spring Boot | 4.1.0 | Parent POM |
| Spring AI BOM | 2.0.0 | |
| Java | 21 | `--enable-preview` |
| `spring-ai-starter-mcp-server-webmvc` | 2.0.0 | SSE transport |
| `spring-ai-openai` | 2.0.0 | Manual `OpenAiEmbeddingModel` in pool (no auto-config) |
| `postgresql` | 42.7.11 | Same as `med-expert-match-ce` |
| `pgvector` | 0.1.6 | |
| `flyway-database-postgresql` | 10.x | |
| `commons-lang3` | 3.20.0 | |
| `caffeine` | 3.2.4 | Stats cache |
| Testcontainers | 2.0.5 | `pgvector/pgvector:pg17` image |
| `spring-boot-starter-jdbc` | (Boot BOM) | NamedParameterJdbcTemplate |
| `spring-boot-starter-actuator` | (Boot BOM) | |

---

## Database Schema

Single table. Vector dimension is **768** (nomic-embed-text:v1.5). HNSW index — at 2,464 rows it builds in seconds and outperforms IVFFlat.

```sql
-- V1__init_medical_cases.sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE medical_case (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    sample_name       TEXT        NOT NULL,
    description       TEXT,
    transcription     TEXT,
    medical_specialty VARCHAR(120),
    keywords          TEXT,
    split             VARCHAR(12),   -- 'train' | 'test' | 'validation'
    embedding         VECTOR(768),   -- nomic-embed-text:v1.5
    fts               TSVECTOR GENERATED ALWAYS AS (
                          to_tsvector('english',
                              coalesce(sample_name,   '') || ' ' ||
                              coalesce(description,   '') || ' ' ||
                              coalesce(transcription, '') || ' ' ||
                              coalesce(keywords,      ''))
                      ) STORED,
    created_at        TIMESTAMPTZ DEFAULT now()
);

-- ANN index: HNSW cosine (correct for nomic-embed-text normalised output)
CREATE INDEX idx_medical_case_embedding
    ON medical_case USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Full-text
CREATE INDEX idx_medical_case_fts
    ON medical_case USING GIN (fts);

-- Specialty filter (used in pre-filter before ANN)
CREATE INDEX idx_medical_case_specialty
    ON medical_case (medical_specialty);

-- Trigram index for LIKE / iLIKE specialty search
CREATE INDEX idx_medical_case_specialty_trgm
    ON medical_case USING GIN (medical_specialty gin_trgm_ops);
```

**Embedding text strategy** — concatenate fields in descending clinical weight, matching the `med-expert-match-ce` prompts pattern:

```
{sample_name}. {description} {keywords}
```

`transcription` is intentionally excluded from the embedding input — it is too long (avg 600+ tokens) and would dominate the vector signal. FTS over `transcription` covers keyword retrieval.

---

## Domain Records (`medical-mcp-domain`)

```java
// Zero framework dependencies — pure Java 21

public record MedicalCase(
    UUID   id,
    String sampleName,
    String description,
    String transcription,
    String medicalSpecialty,
    String keywords,
    String split,
    Instant createdAt
) {}

public record CaseSummary(         // Returned by list/search tools (no transcription)
    UUID   id,
    String sampleName,
    String description,
    String medicalSpecialty,
    String keywords,
    String split
) {}

public record SemanticMatch(
    CaseSummary cas,
    double similarity
) {}

public record SpecialtyCount(
    String specialty,
    long   count
) {}

public record DatasetStats(
    long totalCases,
    Map<String, Long> bySpecialty,
    Map<String, Long> bySplit
) {}
```

---

## Infrastructure Layer (`medical-mcp-infrastructure`)

### `MedicalCaseRepository` — NamedParameterJdbcTemplate only

```java
@Repository
public class MedicalCaseRepository {

    // findById → full MedicalCase (includes transcription)
    // findAll(int limit, int offset) → List<CaseSummary>
    // fullTextSearch(String query, String specialty, String split, int limit) → List<CaseSummary>
    //   SQL: WHERE fts @@ plainto_tsquery('english', :query)
    //        AND  (:specialty IS NULL OR medical_specialty = :specialty)
    //        ORDER BY ts_rank(fts, plainto_tsquery('english', :query)) DESC
    // listSpecialties() → List<SpecialtyCount>
    // countAll() → long
}
```

### `PgVectorSearchService` — cosine similarity + pre-filter

```java
@Service
public class PgVectorSearchService {

    // semanticSearch(float[] embedding, String specialty, int topK, double minSimilarity)
    //   SQL:
    //     SELECT id, sample_name, description, medical_specialty, keywords, split,
    //            1 - (embedding <=> :emb::vector) AS similarity
    //     FROM   medical_case
    //     WHERE  (:specialty IS NULL OR medical_specialty = :specialty)
    //       AND  1 - (embedding <=> :emb::vector) >= :minSimilarity
    //     ORDER  BY embedding <=> :emb::vector   -- use operator for index use
    //     LIMIT  :topK
}
```

### `MultiEndpointEmbeddingProperties`

```java
@ConfigurationProperties(prefix = "medicalmcp.embedding.multi-endpoint")
@Validated
public class MultiEndpointEmbeddingProperties {
    @NotEmpty
    @Valid
    List<EndpointConfig> endpoints;  // minimum 1; url @NotBlank per entry
    int skipDurationMin = 10;
    int workerPerEndpoint = 1;
    int apiBatchSize = 50;

    static class EndpointConfig {
        @NotBlank String url;
        String model;
        int priority = 0;
        Integer workers;
    }
}
```

### `EmbeddingEndpointPool` — multi-endpoint worker pool

Port of [`med-expert-match-ce` `EmbeddingEndpointPool`](https://github.com/berdachuk/med-expert-match-ce/blob/main/src/main/java/com/berdachuk/medexpertmatch/embedding/multiendpoint/EmbeddingEndpointPool.java):

```java
// medical-mcp-infrastructure/.../embedding/multiendpoint/EmbeddingEndpointPool.java

// Shared task queue; worker-per-endpoint threads pull batches
// embed(String) → CompletableFuture<List<Double>>
// embedBatch(List<String>) → List<CompletableFuture<...>>  (groups by api-batch-size)
// Failed endpoint skipped for skip-duration-min, then auto-retried
// @PreDestroy shutdown with 30s drain
```

### `EmbeddingEndpointPoolConfig` — manual OpenAiEmbeddingModel wiring

```java
@Configuration
public class EmbeddingEndpointPoolConfig {

    @Bean
    EmbeddingEndpointPool embeddingEndpointPool(
            MultiEndpointEmbeddingProperties properties,
            Environment environment) {
        // Read spring.ai.custom.embedding.api-key + dimensions
        // Sort endpoints by priority; skip entries with blank url
        // Per endpoint: OpenAiEmbeddingModel(MetadataMode.EMBED, OpenAiEmbeddingOptions.builder()...)
        // normalizeOpenAiBaseUrl(url) → append /v1 if missing
        // throw IllegalStateException if no valid endpoints remain after filtering
        return new EmbeddingEndpointPool(endpointStates, workersPerEndpoint,
                properties.getSkipDurationMin(), properties.getApiBatchSize());
    }
}
```

### `MedicalEmbeddingService` — delegates to pool

```java
@Service
public class MedicalEmbeddingService {

    private final EmbeddingEndpointPool pool;  // required dependency — pool always present

    // embed(String text) → float[]           — pool.embed(text).get(timeout)
    // batchEmbed(List<String>) → List<float[]> — pool.embedBatch(texts), join futures
    // buildEmbeddingInput(CaseSummary) → "{sampleName}. {description} {keywords}"
}
```

### `DatasetLoader` — CommandLineRunner, idempotent

Runs once at startup; skips if `SELECT COUNT(*) FROM medical_case > 0`.

```
Flow:
  1. Download Parquet from HuggingFace CDN
     https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial
     /resolve/main/data/train-*.parquet  (+ test / validation)
  2. Parse with DuckDB JDBC or Apache Arrow (single dependency)
  3. Batch INSERT into medical_case (skip embedding column)
  4. For each row: build embedding input text → call MedicalEmbeddingService
  5. Batch UPDATE embedding column (50 rows per batch — matches med-expert-match-ce api-batch-size)
  6. Log progress every 100 rows
```

Loader is skippable via property:

```yaml
medicalmcp:
  dataset:
    loader:
      enabled: ${MEDICALMCP_DATASET_LOADER_ENABLED:true}
      batch-size: ${MEDICALMCP_DATASET_LOADER_BATCH_SIZE:50}
```

---

## MCP Tools Layer (`medical-mcp-tools`)

All beans are `@Component`. Annotation scanner picks them up automatically — no explicit registration.

### `MedicalCaseTools`

```java
@Component
public class MedicalCaseTools {

    @McpTool(
        name = "search_cases",
        description = "Full-text search over medical case transcriptions, descriptions, and keywords.",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    List<CaseSummary> searchCases(
        McpSyncRequestContext ctx,
        @McpToolParam(description = "Search terms", required = true) String query,
        @McpToolParam(description = "Filter by medical specialty (exact match)", required = false) String specialty,
        @McpToolParam(description = "Filter by dataset split: train | test | validation", required = false) String split,
        @McpToolParam(description = "Max results (default 10, max 50)", required = false) Integer limit
    );

    @McpTool(
        name = "get_case",
        description = "Retrieve a single medical case by UUID, including the full transcription text.",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false),
        generateOutputSchema = true
    )
    MedicalCase getCase(
        @McpToolParam(description = "Case UUID", required = true) String id
    );

    @McpTool(
        name = "semantic_search",
        description = "Vector similarity search over medical cases. Embeds the query and returns the most similar cases by cosine distance.",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    List<SemanticMatch> semanticSearch(
        McpSyncRequestContext ctx,
        @McpToolParam(description = "Free-text query to embed and compare", required = true) String query,
        @McpToolParam(description = "Pre-filter by medical specialty before ANN search", required = false) String specialty,
        @McpToolParam(description = "Number of results (default 5)", required = false) Integer topK,
        @McpToolParam(description = "Minimum cosine similarity 0–1 (default 0.70)", required = false) Double minSimilarity
    );
    // Progress: 0% → embedding → 50% → pgvector → 100%

    @McpTool(
        name = "list_specialties",
        description = "List all 13 medical specialties present in the dataset with case counts.",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false)
    )
    List<SpecialtyCount> listSpecialties();

    @McpTool(
        name = "get_dataset_stats",
        description = "Return dataset statistics: total cases, breakdown by specialty and by split (train/test/validation).",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false)
    )
    DatasetStats getDatasetStats();
    // Cached 60 s via Caffeine
}
```

### `MedicalCaseResources`

```java
@Component
public class MedicalCaseResources {

    @McpResource(
        uri = "medical://cases/{id}",
        name = "medical-case",
        description = "Full medical case record (including transcription) by UUID.",
        mimeType = "application/json",
        annotations = @McpResource.McpAnnotations(readOnlyHint = true, priority = 0.9f)
    )
    MedicalCase getCase(String id);

    @McpResource(
        uri = "medical://stats",
        name = "medical-dataset-stats",
        description = "Dataset statistics snapshot.",
        mimeType = "application/json",
        annotations = @McpResource.McpAnnotations(readOnlyHint = true, priority = 0.5f)
    )
    DatasetStats getStats();
}
```

### `MedicalCasePrompts`

```java
@Component
public class MedicalCasePrompts {

    @McpPrompt(
        name = "case-analysis",
        description = "Structured prompt for LLM analysis of a retrieved medical case."
    )
    List<PromptMessage> analyzeCase(
        @McpArg(name = "caseId",  description = "UUID of the medical case", required = true) String caseId,
        @McpArg(name = "focus",   description = "Analysis focus: diagnosis | treatment | keywords | summary", required = false) String focus
    );
    // Fetches MedicalCase from DB, injects into prompt template
}
```

### `MedicalCaseCompletions`

```java
@Component
public class MedicalCaseCompletions {

    @McpComplete(prompt = "case-analysis")   // completes caseId argument
    CompleteResponse completeCaseId(String prefix);
    // SELECT id, sample_name FROM medical_case WHERE sample_name ILIKE :prefix% LIMIT 10
}
```

---

## Application Configuration (`medical-mcp-app`)

### `application.yml`

```yaml
spring:
  application:
    name: medical-mcp-server
  autoconfigure:
    exclude:
      # Disable Spring AI OpenAI auto-config; embeddings wired manually (med-expert-match-ce pattern)
      - org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration
  datasource:
    url: jdbc:postgresql://${MEDICALMCP_DB_HOST:localhost}:5432/${MEDICALMCP_DB_NAME:medical_mcp}
    username: ${MEDICALMCP_DB_USERNAME:medical_mcp}
    password: ${MEDICALMCP_DB_PASSWORD:medical_mcp}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 3
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1800000
      # hnsw.ef_search controls recall vs speed for ANN queries; 40 is a good default at 2464 rows
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
          Wraps the hpe-ai/medical-cases-classification-tutorial dataset (2,464 rows, 13 specialties).
          Use search_cases for keyword/FTS queries; semantic_search for similarity queries;
          get_case to retrieve a full transcription by UUID; list_specialties to explore labels.
          Embeddings: nomic-embed-text:v1.5 at 768 dims — same model as med-expert-match-ce GraphRAG.
        sse-endpoint: /sse
        sse-message-endpoint: /mcp/message
        keep-alive-interval: 30s
        capabilities:
          tool: true
          resource: true
          prompt: true
          completion: true
        tool-change-notification: false
        resource-change-notification: false
        annotation-scanner:
          enabled: true

server:
  port: ${SERVER_PORT:8092}
  shutdown: graceful
  compression:
    enabled: true
    mime-types: application/json
    min-response-size: 1024

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
  metrics:
    export:
      prometheus:
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

logging:
  level:
    com.example.medicalmcp: INFO
    com.example.medicalmcp.infra.embedding: INFO
    org.springframework.ai.mcp: DEBUG
    org.springframework.ai.openai: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Security

For local / dev use (matching the current `med-expert-match-ce` local profile):

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/sse", "/mcp/message",
                             "/actuator/health", "/actuator/info").permitAll()
            .anyRequest().authenticated()
        )
        // Swap for JWT when deploying behind med-expert-match-ce API gateway:
        // .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .httpBasic(AbstractHttpConfigurer::disable)
        .build();
}
```

---

## Environment Variables

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
| `SERVER_PORT` | `8092` | MCP server port |
| `MEDICALMCP_DATASET_LOADER_ENABLED` | `true` | Set to `false` after first load |
| `MEDICALMCP_DATASET_LOADER_BATCH_SIZE` | `50` | JDBC insert batch size (pass 1) |
| `MEDICALMCP_RETRIEVAL_SIMILARITY_THRESHOLD` | `0.70` | Default `minSimilarity` for `semantic_search` |
| `MEDICALMCP_RETRIEVAL_DEFAULT_TOP_K` | `5` | Default `topK` for `semantic_search` |
| `MEDICALMCP_RETRIEVAL_MAX_LIMIT` | `50` | Max `limit` for `search_cases` |
| `MEDICALMCP_CACHE_STATS_TTL` | `60` | Stats cache TTL (seconds) |

---

## Docker Compose

```yaml
services:
  medical-mcp-server:
    build: ./medical-mcp-app
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

> Ollama runs on the host, not in Docker. `host.docker.internal` resolves correctly on Linux with `--add-host` or Docker Desktop on Mac.

---

## MCP Client Connection

### Claude Desktop (`~/.config/claude/claude_desktop_config.json`)

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

### `med-expert-match-ce` (`application.yml`)

```yaml
spring:
  ai:
    mcp:
      client:
        sse:
          connections:
            medical-dataset:
              url: http://localhost:8092/sse
```

---

## Milestones

| # | Milestone | Key deliverables | Status |
|---|---|---|---|
| **M1** | Schema + domain | `V1__init_medical_cases.sql`, domain records (`MedicalCase`, `CaseSummary`, `SemanticMatch`, `SpecialtyCount`, `DatasetStats`), `MedicalMcpApplication` stub | ⬜ |
| **M2** | Dataset loader pass 1 | `DatasetLoader` (Parquet → JDBC batch insert, `embedding = NULL`), `MedicalCaseRepository` insert only | ⬜ |
| **M3** | Repositories | `MedicalCaseRepository` (FTS + findById + listSpecialties), `PgVectorSearchService` (cosine), `DatasetStats` query | ⬜ |
| **M4** | Embedding wiring | `EmbeddingEndpointPool`, `EmbeddingEndpointPoolConfig`, `MedicalEmbeddingService`, loader phase 2, integration test with `pgvector/pgvector:pg17` | ⬜ |
| **M5** | MCP surface | `MedicalCaseTools` ×5, `MedicalCaseResources` ×2, `MedicalCasePrompts`, `MedicalCaseCompletions` | ⬜ |
| **M6** | Config + security | `application.yml`, `SecurityConfig`, Caffeine cache, `medicalmcp.embedding.multi-endpoint` + `medicalmcp.*` properties | ⬜ |
| **M7** | End-to-end | Claude Desktop smoke test, `McpSyncClient` call from `med-expert-match-ce` | ⬜ |
| **M8** | Docker + docs | `docker-compose.yml`, `README.md`, startup guide | ⬜ |

---

## Key Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Embedding transport | Always-on `EmbeddingEndpointPool` + manual `OpenAiEmbeddingModel` | Mandatory for all embed paths; ≥1 endpoint required at startup; no Spring AI embedding auto-config |
| Embedding model | `nomic-embed-text:v1.5` @ 768 dims | Same as `med-expert-match-ce` — vectors are cross-compatible, no re-embedding needed if DB is shared |
| Embedding input | `{sampleName}. {description} {keywords}` | Transcription too long for single-vector embedding; FTS covers it instead |
| ANN index | HNSW (not IVFFlat) | 2,464 rows — HNSW builds in < 1 s, no need for IVFFlat `lists` tuning |
| JDBC only | `NamedParameterJdbcTemplate` | Consistent with `med-expert-match-ce`; avoids JPA overhead and Hibernate schema management |
| Loader idempotency | `COUNT(*) > 0` guard | Safe restart — no duplicate rows, no flag table |
| MCP transport | Spring MVC SSE (SYNC) | No reactive stack; simpler threading model; matches existing MCP server pattern |
| Stats cache | Caffeine in-memory 60 s | Avoids aggregate query on every stats call; dataset is static post-load |
| Security default | Permit-all for SSE + MCP | Local/dev default; JWT comment left in place for prod wiring |

---

## Domain model summary

| Record | Used by |
|---|---|
| `MedicalCase` | `get_case`, resources, prompts |
| `CaseSummary` | `search_cases`, FTS/vector result bodies |
| `SemanticMatch` | `semantic_search` — `{ case: CaseSummary, similarity }` |
| `SpecialtyCount` | `list_specialties` |
| `DatasetStats` | `get_dataset_stats`, `medical://stats` resource |
