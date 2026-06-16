# M-15 — Prompt-lab live chat & test gate (optional)

**Status:** ⬜ Active  
**Date:** 2026-06-16  
**Milestone:** M9 extended — [docs/future/prompt-lab.md §7](../../docs/future/prompt-lab.md#7-evaluation-methodology-aligned-with-04-testingmd)

## Objective

Wire live LLM classification for prompt-lab eval and add test-split quality gate alongside retrieval benchmarks.

## Prerequisites

- [M-12](archive/M-12-meta-prompting-lab.md) — MCP lab tools + meta-improvement with offline stub

## Deliverables

| Artifact | Path |
|---|---|
| Live chat client | `ChatPromptLabClassificationClient` + `spring-ai` chat deps |
| Config | `medicalmcp.prompt-lab.chat.*` in `application-prompt-lab.yml` |
| Test gate IT | `PromptLabGateIntegrationTest` on test CSV split |
| Quality profile | Optional `mvn verify -Pquality -Pprompt-lab` |
| Docs | Update [prompt-lab-user-guide.md](../../docs/guides/prompt-lab-user-guide.md) |

## Acceptance criteria

- [ ] `chat.enabled=true` uses Ollama/OpenAI-compatible endpoint for eval MCP tools
- [ ] `chat.enabled=false` (CI default) unchanged — offline stub
- [ ] Test-split gate IT passes on fixture or documented skip
- [ ] Default MCP surface unchanged without `prompt-lab` profile

## Out of scope

- Production classification API
- Default-profile MCP tools

## References

- [docs/future/prompt-lab.md](../../docs/future/prompt-lab.md)
- [04-testing.md](../../docs/04-testing.md)
