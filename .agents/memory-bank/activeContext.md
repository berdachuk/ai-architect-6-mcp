# Active context

**Updated:** 2026-06-16

## Current focus

**M10 complete.** Core track M1–M8 and optional M9/M10 are complete.

Next optional extended lab: **M9 ext** — meta-prompting MCP tools. Plan: [.agents/plans/M-12-meta-prompting-lab.md](../plans/M-12-meta-prompting-lab.md).

## Next steps (optional M-12)

1. `MetaPromptImprovementService` with failure-context meta prompt
2. `PromptLabTools` MCP under `prompt-lab` profile
3. Live chat-client eval path + optional CI nightly

## Verified

- `focus=specialty` on `case-analysis` uses promoted `react_self_reflection` block (M10)
- `promptlab` offline eval + `mvn verify -Pprompt-lab` (M9)
- Docker/CI/quality gate (M8); E2E SSE smoke (M7)
- Default MCP surface unchanged (5 tools, 2 resources, 1 prompt)
