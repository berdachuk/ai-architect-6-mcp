# M-13 — User guides (MCP, Prompt lab, LM Studio)

**Status:** ✅ Complete (archived 2026-06-16)  
**Date:** 2026-06-16  
**Type:** Documentation (user-facing)

## Objective

Add practical markdown user guides for MCP features, prompt-lab workflow, and LM Studio manual testing.

## Deliverables

| Artifact | Path |
|---|---|
| Guides index | `docs/guides/README.md` |
| MCP user guide | `docs/guides/mcp-user-guide.md` |
| Prompt lab guide | `docs/guides/prompt-lab-user-guide.md` |
| LM Studio manual test | `docs/guides/lm-studio-mcp-manual-test.md` |
| Doc cross-links | `docs/README.md`, `README.md`, `05-deployment.md`, `AGENTS.md` |

## Acceptance criteria

- [x] Every production tool/resource/prompt documented with examples
- [x] All `focus` values described; specialty promotion called out
- [x] `mvn verify -Pprompt-lab` and config documented
- [x] LM Studio `mcp.json` + smoke checklist
- [x] Links from deployment and doc index

## Next

- [M-14 Claude Desktop guide](../M-14-claude-desktop-mcp.md) (optional docs)
- Extend prompt-lab guide when [M-12](M-12-meta-prompting-lab.md) ships
