# M-06 — MCP module (M5)

**Status:** ⬜ Active  
**Date:** 2026-06-16  
**Milestone:** M5 — [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones)

## Objective

Expose dataset-backed MCP surface: 5 tools, 2 resources, 1 prompt — delegate to service interfaces only ([DEC-004](../memory-bank/decisions.md#dec-004--mcp-surface-5-tools-0-completions)).

## Prerequisites

- [M-05](archive/M-05-embedding-module.md) complete — `semanticSearch`, pass-2 embeddings, SQL externalized ([DEC-010](../memory-bank/decisions.md#dec-010--external-sql-files-with-injectsql))

## Deliverables

| Artifact | Path |
|---|---|
| MCP tools | `mcp/MedicalCaseTools.java` — `search_cases`, `get_case`, `semantic_search`, `list_specialties`, `get_dataset_stats` |
| MCP resources | `mcp/MedicalCaseResources.java` — `medical://cases/{id}`, `medical://stats` |
| MCP prompt | `mcp/MedicalCasePrompts.java` — `case-analysis` |
| Spring AI MCP config | `application.yml` SSE transport `:8092` |
| Contract IT | `McpToolsContractIntegrationTest`, `McpResourcesIntegrationTest` |

## Requirement traceability

| ID | Summary | Module | Test |
|---|---|---|---|
| REQ-006 | MCP search/stats tools | `mcp` → `retrieval`, `medicalcase`, `embedding` | MCP contract IT |
| DEC-004 | 5 tools, 0 completions | `mcp` | Contract IT |

## Acceptance criteria

- [ ] All tools callable; responses match `CaseSummary` / `MedicalCase` / `SemanticMatch` shapes
- [ ] No `@McpComplete`; no REST API in default profile
- [ ] MCP injects `VectorSearchService`, `MedicalCaseRepository`, `EmbeddingService` — never `*.impl.*`
- [ ] `mvn verify -Pintegration` passes (WSL)

## References

- [docs/03-design.md § MCP](../../docs/03-design.md)
- [docs/04-testing.md §5.4](../../docs/04-testing.md)

## Next

[M-07 config + security](M-07-config-security.md) (requirements M6).
