# Project brief

## Identity

| Field | Value |
|---|---|
| Name | `medical-mcp-server` |
| Type | Spring Boot MCP server (SSE) |
| Dataset | [hpe-ai/medical-cases-classification-tutorial](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial) — 2,464 rows, 13 specialties |
| Reference pattern | [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce) |

## Goal

Load the HuggingFace medical cases CSV into PostgreSQL + pgvector and expose **dataset-faithful** search/retrieval via MCP (tools, resources, prompts) for Claude Desktop and `med-expert-match-ce`.

## Scope (M1–M8)

- Flyway schema, Modulith package modules, JDBC repositories
- Mandatory `EmbeddingEndpointPool` (`nomic-embed-text:v1.5`, 768 dims)
- MCP: 5 tools, 2 resources, 1 prompt, **0 completions**
- Retrieval quality gates on held-out **test** split

## Out of scope (production)

- REST API, UI, classifier training, MCP completions, HuggingFace sync
- Optional M9/M10 prompt-lab: [docs/future/prompt-lab.md](../docs/future/prompt-lab.md)

## Stakeholders

- Implementers / AI agents building from [docs/](../docs/README.md)
- Consumers: Claude Desktop, `med-expert-match-ce` (`McpSyncClient`)

## Canonical docs

Deep reference: [docs/README.md](../docs/README.md) — do not duplicate here.