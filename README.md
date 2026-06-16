# medical-mcp-server

Spring AI 2.0 MCP server that wraps the [hpe-ai/medical-cases-classification-tutorial](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial) dataset (2,464 medical cases, 13 specialties) and exposes search and retrieval tools over SSE.

## Documentation

| Document | Description |
|---|---|
| [docs/PRD.md](docs/PRD.md) | Product requirements, MCP surface, NFRs, configuration |
| [docs/PLAN.md](docs/PLAN.md) | Architecture, class design, implementation milestones |

## Stack

- Java 21, Spring Boot 4.1.0, Spring AI 2.0.0
- PostgreSQL 17 + pgvector (HNSW cosine, FTS)
- Embeddings: `nomic-embed-text:v1.5` @ 768 dims via Ollama (OpenAI-compatible client)
- **Embedding transport:** mandatory `EmbeddingEndpointPool` — at least one endpoint required at startup
- MCP port: `8092` (`/sse`)

## Prerequisites

- JDK 21, Maven 3.9+
- PostgreSQL 17 with pgvector
- Ollama with `nomic-embed-text:v1.5` pulled

## Quick start (planned)

```bash
# PostgreSQL + pgvector
docker run -d --name medical-mcp-pg \
  -e POSTGRES_DB=medical_mcp \
  -e POSTGRES_USER=medical_mcp \
  -e POSTGRES_PASSWORD=medical_mcp \
  -p 5432:5432 pgvector/pgvector:pg17

ollama pull nomic-embed-text:v1.5

# After Maven scaffold lands:
mvn clean verify
mvn -pl medical-mcp-app spring-boot:run
```

MCP endpoint: `http://localhost:8092/sse`

## Related projects

- [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce) — GraphRAG consumer; shares embedding model and pool pattern
