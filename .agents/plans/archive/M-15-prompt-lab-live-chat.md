# M-15 — Prompt-lab live chat & test gate (optional)

**Status:** ✅ Complete  
**Date:** 2026-06-16  
**Milestone:** M9 extended — [docs/future/prompt-lab.md §7](../../docs/future/prompt-lab.md#7-evaluation-methodology-aligned-with-04-testingmd)

## Objective

Wire live LLM classification for prompt-lab eval and add test-split quality gate alongside retrieval benchmarks.

## Prerequisites

- [M-12](M-12-meta-prompting-lab.md) — MCP lab tools + meta-improvement with offline stub

## Deliverables

| Artifact | Path |
|---|---|
| Live chat client | `ChatPromptLabClassificationClient` + Spring AI `OpenAiChatModel` |
| Config | `medicalmcp.prompt-lab.chat.*` in `application-prompt-lab.yml` |
| Test gate IT | `PromptLabGateIntegrationTest` on test CSV split |
| Quality profile | `mvn verify -Pprompt-lab-quality` (retrieval + prompt-lab gates) |
| Docs | Update [prompt-lab-user-guide.md](../../docs/guides/prompt-lab-user-guide.md) |

## Acceptance criteria

- [x] `chat.enabled=true` uses Ollama/OpenAI-compatible endpoint for eval MCP tools
- [x] `chat.enabled=false` (CI default) unchanged — offline stub
- [x] Test-split gate IT passes on fixture or documented skip
- [x] Default MCP surface unchanged without `prompt-lab` profile

## Out of scope

- Production classification API
- Default-profile MCP tools

## References

- [docs/future/prompt-lab.md](../../docs/future/prompt-lab.md)
- [04-testing.md](../../docs/04-testing.md)
