# Deployment
## Operations & deployment guide

**Version:** 2.0.0  
**Requirements:** [01-requirements.md](../01-requirements.md) · [02-architecture.md](../02-architecture.md)

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

Step-by-step smoke test: [guides/claude-desktop-mcp.md](guides/claude-desktop-mcp.md).

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

### LM Studio (`%USERPROFILE%\.lmstudio\mcp.json` on Windows)

```json
{
  "mcpServers": {
    "medical-mcp-server": {
      "url": "http://localhost:8092/sse"
    }
  }
}
```

Enable **Developer → Allow remote MCP** in LM Studio (≥ 0.3.17). Step-by-step smoke test: [guides/lm-studio-mcp-manual-test.md](guides/lm-studio-mcp-manual-test.md).

User guides index: [guides/README.md](guides/README.md).

---

## Related documentation

- [01-requirements.md](01-requirements.md) — env vars (§13) and configuration (§12)
- [02-architecture.md](02-architecture.md) — stack and security defaults
- [03-design.md](03-design.md) — service and MCP implementation
- [04-testing.md](04-testing.md) — smoke checklist (M7)
