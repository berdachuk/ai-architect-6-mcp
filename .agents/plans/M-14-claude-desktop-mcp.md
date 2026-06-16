# M-14 — Claude Desktop MCP guide (optional docs)

**Status:** ⬜ Active  
**Date:** 2026-06-16  
**Type:** Documentation (user-facing) — parallel to M-12 code track

## Objective

Add `docs/guides/claude-desktop-mcp.md` — step-by-step manual testing and daily use of medical-mcp-server with **Claude Desktop** over SSE, mirroring the LM Studio guide pattern.

## Prerequisites

- [M-13](archive/M-13-user-guides.md) complete — guides index and MCP user guide
- Server runnable per [05-deployment.md](../../docs/05-deployment.md)

## Deliverables

| Artifact | Path |
|---|---|
| Claude Desktop guide | `docs/guides/claude-desktop-mcp.md` |
| Index update | `docs/guides/README.md` |
| Cross-link | `05-deployment.md` § MCP Client Connection |

## Suggested content

1. Install Claude Desktop and locate config path (Windows / macOS / Linux)
2. `claude_desktop_config.json` snippet for `http://localhost:8092/sse`
3. Restart / reconnect workflow
4. Same 7-step smoke checklist as LM Studio guide (reuse table)
5. Differences vs LM Studio (hosted model, resource attachment, context limits)
6. Troubleshooting (server not listed, CORS, localhost firewall)
7. Link to [mcp-user-guide.md](../../docs/guides/mcp-user-guide.md) for API reference

## Acceptance criteria

- [ ] Reproducible config snippet matches `05-deployment.md`
- [ ] Smoke checklist covers all 5 tools + `case-analysis`
- [ ] No contradiction with MCP surface counts in requirements §6

## Out of scope

- Anthropic API / Claude Code CLI (Desktop only)
- Production auth hardening

## References

- [docs/guides/lm-studio-mcp-manual-test.md](../../docs/guides/lm-studio-mcp-manual-test.md) — template
- [04-testing.md §11](../../docs/04-testing.md#11-manual-smoke-checklist-m7)
