# M-08 — End-to-end smoke (M7)

**Status:** ⬜ Active  
**Date:** 2026-06-16  
**Milestone:** M7 — [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones)

## Objective

Validate full MCP stack against real clients: `McpSyncClient` (med-expert-match-ce) and Claude Desktop checklist.

## Prerequisites

- [M-07](archive/M-07-config-security.md) complete — config and security hardened
- Ollama with `nomic-embed-text:v1.5` on host for semantic smoke

## Deliverables

| Artifact | Path |
|---|---|
| E2E profile | Maven `e2e` profile (optional, manual/CI) |
| Client smoke | `McpSyncClient` integration against running server |
| Checklist | [docs/04-testing.md §11](../../docs/04-testing.md#11-manual-smoke-checklist-m7) — document results |
| Full dataset load | Production loader sources (2464 rows) for smoke env |

## Acceptance criteria

- [ ] Server starts with dataset loaded + embeddings present
- [ ] All 5 tools respond on `:8092/sse`
- [ ] `medical://cases/{id}` and `medical://stats` resources resolve
- [ ] `case-analysis` prompt returns populated template for known UUID
- [ ] Manual checklist items verified (or automated where feasible)

## References

- [docs/use-cases.md](../../docs/use-cases.md)
- [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce)

## Next

[M-09 Docker + quality gate](M-09-docker-quality-gate.md) (requirements M8).
