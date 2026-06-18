# M-16 — CI combined quality gates (optional)

**Status:** ⬜ Active  
**Date:** 2026-06-16  
**Milestone:** Ops — [04-testing.md §10](../../docs/04-testing.md#10-mapping-to-milestones)

## Objective

Run retrieval and prompt-lab test-split gates in CI using the `-Pprompt-lab-quality` Maven profile added in M-15.

## Prerequisites

- [M-15](archive/M-15-prompt-lab-live-chat.md) — `prompt-lab-quality` profile + `PromptLabGateIntegrationTest`

## Deliverables

| Artifact | Path |
|---|---|
| CI job | `.github/workflows/ci.yml` — `prompt-lab-quality` job |
| Docs | Update `AGENTS.md`, [00-index.md](00-index.md), [prompt-lab-user-guide.md](../../docs/guides/prompt-lab-user-guide.md) if needed |

## Acceptance criteria

- [ ] CI runs `mvn verify -Pprompt-lab-quality` on push/PR to `develop`
- [ ] Job passes with offline stub (no live Ollama required)
- [ ] Existing `unit`, `integration`, and `prompt-lab` jobs unchanged

## Out of scope

- Live Ollama in CI
- Production MCP surface changes

## References

- [04-testing.md](../../docs/04-testing.md)
- [docs/guides/prompt-lab-user-guide.md](../../docs/guides/prompt-lab-user-guide.md)
