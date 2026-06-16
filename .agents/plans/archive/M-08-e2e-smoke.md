# M-08 — End-to-end smoke (M7)

**Status:** ✅ Complete (archived 2026-06-16)  
**Date:** 2026-06-16  
**Milestone:** M7 — [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones)

## Objective

Validate full MCP stack against real clients: `McpSyncClient` (med-expert-match-ce) and Claude Desktop checklist.

## Prerequisites

- [M-07](M-07-config-security.md) complete — config and security hardened
- Ollama with `nomic-embed-text:v1.5` on host for semantic smoke (manual full-dataset runs)

## Deliverables

| Artifact | Path |
|---|---|
| E2E profile | Maven `e2e` profile + `application-e2e.yml` (full HF sources) |
| Client smoke | `McpSseSmokeIntegrationTest` — `McpSyncClient` over SSE `@RANDOM_PORT` |
| Test helpers | `McpSseTestClientFactory`, `McpSmokeTestSupport` |
| Checklist | [docs/04-testing.md §11](../../docs/04-testing.md#11-manual-smoke-checklist-m7) — automation notes |

## Requirement traceability

| ID | Summary | Module | Test |
|---|---|---|---|
| REQ-006 | MCP tools/resources/prompt over wire | `mcp` | `McpSseSmokeIntegrationTest` |
| NFR-001 | Client-compatible SSE transport | `mcp` | SSE smoke IT |

## Acceptance criteria

- [x] Server starts with dataset loaded + embeddings present (fixture: 10-row train sample)
- [x] All 5 tools respond on `:8092/sse` (via `McpSyncClient` on random port)
- [x] `medical://cases/{id}` and `medical://stats` resources resolve
- [x] `case-analysis` prompt returns populated template for known UUID
- [x] Manual checklist items automated where feasible; full 2464-row checks deferred to M8/staging

## References

- [docs/use-cases.md](../../docs/use-cases.md)
- [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce)

## Next

[M-09 Docker + quality gate](../M-09-docker-quality-gate.md) (requirements M8).
