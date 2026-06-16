# Architecture
## Software Architecture Document (SAD)

**Version:** 2.0.0  
**Date:** 2026-06-16  
**Requirements:** [01-requirements.md](01-requirements.md)  
**Reference pattern:** [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce)

System design for `medical-mcp-server`: deployment topology, Spring Modulith modules, stack versions, and key design decisions. Implementation details: [03-design.md](03-design.md). Operations: [05-deployment.md](05-deployment.md).

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
    │   │   ├── mcp/                          # MedicalCaseTools, Resources, Prompts
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

## Related documentation

- [01-requirements.md §9](01-requirements.md#9-architecture-spring-modulith) — architectural requirements summary
- [03-design.md](03-design.md) — schema, services, MCP class sketches
- [05-deployment.md](05-deployment.md) — config, Docker, MCP clients
- [01-requirements.md §14](01-requirements.md#14-milestones) — development milestones M1–M8
- [04-testing.md](04-testing.md) — `ModulithArchitectureTest` and quality gates
