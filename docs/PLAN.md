# Architecture & Implementation Plan
## `medical-mcp-server` — HuggingFace Dataset Wrapper

**Version:** 1.6.0  
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
│  medical-mcp-server  :8092  (single module, Spring Modulith)      │
│                                                                 │
│  mcp/ — Spring AI annotation adapters                           │
│  ┌──────────────────┬───────────────────┬──────────────────┐   │
│  │ @McpTool ×5      │ @McpResource ×2   │ @McpPrompt ×1    │   │
│  └────────┬─────────┴─────────┬─────────┴──────────────────┘   │
│           │ injects service interfaces only                     │
│  retrieval/ · dataset/ · embedding/ · medicalcase/              │
│  ┌──────────────────┬───────────────────┬──────────────────┐   │
│  │ VectorSearch     │ DatasetLoader     │ EmbeddingEndpoint│   │
│  │ Service          │ Service           │ Pool + Embedding │   │
│  │ + impl           │ + impl            │ Service (API)    │   │
│  └────────┬─────────┴─────────┬─────────┴──────┬───────────┘   │
│           │                   │                 │               │
│  core/ — config, security, Flyway, Actuator                     │
└───────────┼───────────────────┼─────────────────┼───────────────┘
            │                   │                 │
            ▼                   ▼                 ▼
┌────────────────────┐  ┌────────────────┐  ┌──────────────────┐
│  PostgreSQL 17     │  │  Ollama        │  │  HuggingFace     │
│  + pgvector ext.   │  │  nomic-embed   │  │  CSV files       │
│  medical_case tbl  │  │  text:v1.5     │  │  (one-time load) │
│  HNSW idx 768-dim  │  │  768 dims      │  │                  │
└────────────────────┘  └────────────────┘  └──────────────────┘
```

---

## Module Structure (Spring Modulith)

Single Maven module with **package-based application modules** — same layout as [`med-expert-match-ce`](https://github.com/berdachuk/med-expert-match-ce/tree/main/src/main/java/com/berdachuk/medexpertmatch).

```
medical-mcp-server/
├── pom.xml
├── docker-compose.yml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/example/medicalmcp/
    │   │   ├── MedicalMcpApplication.java
    │   │   ├── core/                         # config, exception, health, util
    │   │   ├── medicalcase/
    │   │   │   ├── domain/                   # records: MedicalCase, CaseSummary, …
    │   │   │   ├── repository/               # MedicalCaseRepository (interface)
    │   │   │   ├── repository/impl/          # MedicalCaseRepositoryImpl (@Repository)
    │   │   │   └── package-info.java         # @ApplicationModule(allowedDependencies = "core :: *")
    │   │   ├── embedding/
    │   │   │   ├── service/EmbeddingService.java
    │   │   │   ├── service/impl/EmbeddingServiceImpl.java
    │   │   │   ├── multiendpoint/            # EmbeddingEndpointPool, EndpointState, …
    │   │   │   ├── config/                   # EmbeddingEndpointPoolConfig, properties
    │   │   │   └── package-info.java
    │   │   ├── retrieval/
    │   │   │   ├── service/VectorSearchService.java
    │   │   │   ├── service/impl/VectorSearchServiceImpl.java
    │   │   │   └── package-info.java
    │   │   ├── dataset/
    │   │   │   ├── service/DatasetLoaderService.java
    │   │   │   ├── service/impl/DatasetLoaderServiceImpl.java
    │   │   │   └── package-info.java
    │   │   ├── mcp/                          # MedicalCaseTools, Resources, Prompts, Completions
    │   │   │   └── package-info.java
    │   │   └── system/                       # EmbeddingPoolHealthIndicator (optional)
    │   └── resources/
    │       ├── application.yml
    │       ├── db/migration/                 # V1__init_medical_cases.sql
    │       └── sql/                          # optional per-module SQL (@InjectSql)
    └── test/java/com/example/medicalmcp/
        └── ModulithArchitectureTest.java
```

### Modulith dependency rules (`@ApplicationModule`)

```java
// medicalcase/package-info.java
@ApplicationModule(allowedDependencies = "core :: *")
package com.example.medicalmcp.medicalcase;

// embedding/package-info.java
@ApplicationModule(allowedDependencies = {"core :: *"})
package com.example.medicalmcp.embedding;

// retrieval/package-info.java
@ApplicationModule(allowedDependencies = {"core :: *", "medicalcase :: *", "embedding :: *"})
package com.example.medicalmcp.retrieval;

// dataset/package-info.java
@ApplicationModule(allowedDependencies = {"core :: *", "medicalcase :: *", "embedding :: *"})
package com.example.medicalmcp.dataset;

// mcp/package-info.java
@ApplicationModule(allowedDependencies = {
    "core :: *", "medicalcase :: *", "retrieval :: *", "embedding :: *", "dataset :: *"
})
package com.example.medicalmcp.mcp;

// system/package-info.java
@ApplicationModule(allowedDependencies = {"core :: *", "embedding :: *"})
package com.example.medicalmcp.system;
```

### Interface / implementation convention

| Pattern | med-expert-match-ce example | medical-mcp-server |
|---|---|---|
| Repository API | `medicalcase/repository/MedicalCaseRepository.java` | Same package layout |
| Repository impl | `medicalcase/repository/impl/MedicalCaseRepositoryImpl.java` | JDBC via `NamedParameterJdbcTemplate` |
| Service API | `embedding/service/EmbeddingService.java` | `EmbeddingService`, `VectorSearchService`, `DatasetLoaderService` |
| Service impl | `embedding/service/impl/MultiEndpointEmbeddingServiceImpl.java` | `*ServiceImpl` in `service/impl/` |
| MCP layer | N/A (REST in med-expert-match-ce) | `mcp/*` — injects interfaces only |

**Rules:** no `@Service` / `@Repository` on interfaces; impl classes are the only `@Component` stereotypes; MCP and cross-module code never references `*.impl.*` types.

### Modulith verification (CI)

```java
@ApplicationModuleTest
class ModulithArchitectureTest {
    @Test
    void verifyModuleBoundaries() {
        ApplicationModules.of(MedicalMcpApplication.class).verify();
    }
}
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
| Spring Modulith BOM | 2.1.0 | Aligned with med-expert-match-ce |
| `spring-modulith-core` | 2.1.0 | `@ApplicationModule` |
| `spring-modulith-starter-test` | 2.1.0 | `ApplicationModuleTest`, `verify()` |
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

### Parent `pom.xml` (single module)

```xml
<properties>
  <java.version>21</java.version>
  <spring-ai.version>2.0.0</spring-ai.version>
  <spring-modulith.version>2.1.0</spring-modulith.version>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-bom</artifactId>
      <version>${spring-ai.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.modulith</groupId>
      <artifactId>spring-modulith-bom</artifactId>
      <version>${spring-modulith.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

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
    split             VARCHAR(12),   -- 'train' | 'validation' | 'test'
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

`transcription` is intentionally excluded from the embedding input — it is too long (avg 600+ tokens) and would dominate the vector signal. FTS over `transcription` covers keyword retrieval. Skip null/empty `keywords` when building embed text (~36 % of HF rows).

---

## Domain Records (`medicalcase/domain`)

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
    CaseSummary caseSummary,
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

## Application modules (implementation)

### `medicalcase` — repository API + JDBC impl

```java
// medicalcase/repository/MedicalCaseRepository.java — public API
public interface MedicalCaseRepository {
    Optional<MedicalCase> findById(UUID id);
    List<CaseSummary> fullTextSearch(String query, String specialty, String split, int limit);
    List<SpecialtyCount> listSpecialties();
    long countAll();
    void insertBatch(List<MedicalCase> cases);
    void updateEmbeddingsBatch(Map<UUID, float[]> embeddings);
}

// medicalcase/repository/impl/MedicalCaseRepositoryImpl.java
@Repository
public class MedicalCaseRepositoryImpl implements MedicalCaseRepository {
    private final NamedParameterJdbcTemplate jdbc;
    // SQL via @InjectSql("classpath:sql/medicalcase/...") or inline constants
}
```

### `retrieval` — vector + stats service

```java
// retrieval/service/VectorSearchService.java
public interface VectorSearchService {
    List<SemanticMatch> semanticSearch(float[] embedding, String specialty, int topK, double minSimilarity);
    DatasetStats getDatasetStats();
}

// retrieval/service/impl/VectorSearchServiceImpl.java
@Service
public class VectorSearchServiceImpl implements VectorSearchService {
    private final MedicalCaseRepository repository;
    // pgvector cosine SQL; stats aggregates via repository
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
// embedding/multiendpoint/EmbeddingEndpointPool.java

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

### `EmbeddingService` — delegates to pool

```java
// embedding/service/EmbeddingService.java — public API (same role as med-expert-match-ce)
public interface EmbeddingService {
    float[] embedAsFloatArray(String text);
    List<float[]> embedBatch(List<String> texts);
    String buildEmbeddingInput(CaseSummary summary);
}

// embedding/service/impl/EmbeddingServiceImpl.java
@Service
public class EmbeddingServiceImpl implements EmbeddingService {
    private final EmbeddingEndpointPool pool;  // required — pool always present
    // embed → pool.embed(text); batch → pool.embedBatch(texts)
}
```

### `dataset` — loader service

```java
// dataset/service/DatasetLoaderService.java
public interface DatasetLoaderService {
    void loadIfEmpty();
}

// dataset/service/impl/DatasetLoaderServiceImpl.java — CommandLineRunner delegate
@Service
public class DatasetLoaderServiceImpl implements DatasetLoaderService {
    private final MedicalCaseRepository repository;
    private final EmbeddingService embeddingService;
    // idempotent COUNT(*) > 0 guard; two-pass load
}
```

**Loader flow** (idempotent — skips when `COUNT(*) > 0`):

```
Pass 1:
  1. Download CSV from HuggingFace (medical_cases_train.csv, medical_cases_validation.csv, medical_cases_test.csv)
  2. Parse CSV; assign UUID; set split from filename
  3. Batch INSERT into medical_case (embedding = NULL)

Pass 2:
  4. For each row: build embedding input → EmbeddingService.buildEmbeddingInput()
  5. embedBatch via EmbeddingEndpointPool (api-batch-size 50)
  6. Batch UPDATE embedding column; log progress every 100 rows
```

```yaml
medicalmcp:
  dataset:
    loader:
      enabled: ${MEDICALMCP_DATASET_LOADER_ENABLED:true}
      batch-size: ${MEDICALMCP_DATASET_LOADER_BATCH_SIZE:50}
```

---

## MCP Layer (`mcp/`)

All beans are `@Component`. Inject **service interfaces** (`VectorSearchService`, `EmbeddingService`, `MedicalCaseRepository`) — never impl types.

```java
@Component
public class MedicalCaseTools {

    private final MedicalCaseRepository caseRepository;
    private final VectorSearchService vectorSearch;
    private final EmbeddingService embeddingService;

    @McpTool(
        name = "search_cases",
        description = "Full-text search over medical case transcriptions, descriptions, and keywords.",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true)
    )
    List<CaseSummary> searchCases(
        McpSyncRequestContext ctx,
        @McpToolParam(description = "Search terms", required = true) String query,
        @McpToolParam(description = "Exact medical_specialty label (one of 13 HF classes)", required = false) String specialty,
        @McpToolParam(description = "Filter by dataset split: train | validation | test", required = false) String split,
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
        @McpToolParam(description = "Exact medical_specialty label (one of 13 HF classes)", required = false) String specialty,
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
        description = "Return dataset statistics: total cases, breakdown by specialty and by split (train/validation/test).",
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
        description = "Structured prompt for LLM analysis of a medical case (dataset fields only)."
    )
    List<PromptMessage> analyzeCase(
        @McpArg(name = "caseId",  description = "Server UUID from search_cases / semantic_search", required = true) String caseId,
        @McpArg(name = "focus",   description = "Dataset field emphasis: description | transcription | keywords | specialty | all", required = false) String focus
    );
    // Loads MedicalCase by UUID; focus maps to HF columns only (no diagnosis/treatment inference)
}
```

No `@McpComplete` — `sample_name` is not unique and must not be conflated with `caseId` (UUID).

---

## Application Configuration (`core/` + `src/main/resources`)

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
        version: 1.6.0
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
          completion: false
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
    com.example.medicalmcp.embedding: INFO
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

Aligned with [PRD.md §14](PRD.md#14-milestones) and [TESTING.md §10](TESTING.md#10-mapping-to-milestones).

| # | Milestone | Key deliverables | Tests | Status |
|---|---|---|---|---|
| **M1** | Schema + modulith foundation | `V1__init_medical_cases.sql`, domain records, `package-info.java` per module, Boot stub | `ModulithArchitectureTest`, `FlywaySchemaIntegrationTest` | ⬜ |
| **M2** | Dataset loader pass 1 | `DatasetLoaderService` + impl, `MedicalCaseRepository` + impl (insert), CSV → JDBC | `DatasetLoaderIntegrationTest` (train CSV sample) | ⬜ |
| **M3** | Retrieval module | `MedicalCaseRepository` (FTS, findById, listSpecialties), `VectorSearchService` + impl, stats | Repository IT, `FtsRetrievalQualityTest` (subset) | ⬜ |
| **M4** | Embedding module | `EmbeddingService` + impl, `EmbeddingEndpointPool`, loader pass 2 | Embedding IT, `SemanticRetrievalQualityTest` | ⬜ |
| **M5** | MCP module | `MedicalCaseTools` ×5, resources, `case-analysis` prompt | `McpToolsContractIntegrationTest`, `McpResourcesIntegrationTest` | ⬜ |
| **M6** | Config + security | `application.yml`, `SecurityConfig`, Caffeine cache, `medicalmcp.*` properties | Config binding, cache TTL | ⬜ |
| **M7** | End-to-end | Claude Desktop smoke, `McpSyncClient` from med-expert-match-ce | E2E smoke checklist | ⬜ |
| **M8** | Docker + docs | `docker-compose.yml`, full doc set | Nightly test-split quality gate | ⬜ |

**Optional (future):** M9 prompt-lab, M10 prompt integration — [PROMPT_IMPROVEMENT.md](PROMPT_IMPROVEMENT.md), [PRD §18](PRD.md#18-future-scope-optional).

---

## Key Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Dataset fidelity | CSV source + exact HF schema | 5 columns only; UUID/split/embedding added server-side |
| MCP surface | Tools/resources/prompts only | No completions; prompt `focus` maps to dataset columns |
| Modularity | Spring Modulith package modules | Same pattern as med-expert-match-ce; `verify()` in CI |
| API surface | Interface in `service/` / `repository/` | Impl in `*/impl/`; MCP layer never touches JDBC |
| Embedding transport | Always-on `EmbeddingEndpointPool` + manual `OpenAiEmbeddingModel` | Mandatory for all embed paths; ≥1 endpoint required at startup |
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
| `SemanticMatch` | `semantic_search` — `{ caseSummary: CaseSummary, similarity }` |
| `SpecialtyCount` | `list_specialties` |
| `DatasetStats` | `get_dataset_stats`, `medical://stats` resource |

---

## Related documentation

- [PRD.md](PRD.md) — requirements source of truth
- [USE_CASES.md](USE_CASES.md) — use case catalog
- [TESTING.md](TESTING.md) — test strategy and quality gates
- [PROMPT_IMPROVEMENT.md](PROMPT_IMPROVEMENT.md) — optional prompt-lab (M9/M10)
