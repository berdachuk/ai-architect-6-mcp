# Tech context

## Stack

| Component | Version |
|---|---|
| Java | 21 (`--enable-preview`) |
| Spring Boot | 4.1.0 |
| Spring AI | 2.0.0 |
| Spring Modulith | 2.1.0 |
| PostgreSQL | 17 + pgvector |
| JDBC | `NamedParameterJdbcTemplate` only — **no JPA** |
| Embeddings | Ollama `nomic-embed-text:v1.5` @ 768 via `EmbeddingEndpointPool` |
| MCP transport | Spring MVC SSE, port `8092` |

Detail: [docs/02-architecture.md § Stack](../docs/02-architecture.md#stack--versions)

## Build & test (planned)

```bash
mvn test                          # unit + Modulith
mvn verify -Pintegration          # Testcontainers (pgvector/pg17)
mvn verify -Pquality              # test-split retrieval benchmarks
mvn spring-boot:run               # local :8092/sse
```

Profiles: [docs/04-testing.md §3](../docs/04-testing.md#3-test-pyramid)

## Infrastructure

- DB: `pgvector/pgvector:pg17`
- Ollama on host for embeddings (not in default Docker Compose)
- Env vars: [docs/01-requirements.md §13](../docs/01-requirements.md#13-environment-variables), [docs/05-deployment.md](../docs/05-deployment.md)

### Windows + Docker

On Windows dev machines, **use WSL** to access Docker (Docker Desktop WSL2 backend). Run `docker` CLI, `docker compose`, and `mvn verify -Pintegration` (Testcontainers) from a **WSL shell** — not native PowerShell/CMD. Clone or work under the WSL filesystem (`~/projects/...`) when possible for better I/O; otherwise `cd` to `/mnt/c/...` from WSL.

## Local prerequisites

- JDK 21, Maven 3.9+
- PostgreSQL 17 + pgvector OR Docker (via **WSL** on Windows)
- `ollama pull nomic-embed-text:v1.5`

## Repository state

| Artifact | Status |
|---|---|
| `docs/` v2.0.0 | ✅ Complete |
| `pom.xml` / `src/` | ✅ M1–M8 + M9–M15 + M-17 complete |
| `.agents/` AI context | ✅ Bootstrapped |
| CI workflow | ✅ Configured (ci.yml + quality.yml) |
