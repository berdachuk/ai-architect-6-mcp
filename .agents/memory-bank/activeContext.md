# Active context

**Updated:** 2026-06-16

## Current focus

**M9 complete.** Next optional: **M10** — wire promoted prompt into `MedicalCasePrompts`. Plan: [.agents/plans/M-11-prompt-integration.md](../plans/M-11-prompt-integration.md).

Core track M1–M8 and optional M9 are complete.

## Next steps (optional M10)

1. Select winning template from prompt-lab eval
2. Update `MedicalCasePrompts` with promoted specialty-classification hints
3. Regression tests for prompt structure (no LLM quality assertions)

## Verified

- `promptlab` module + `mvn verify -Pprompt-lab` offline eval (M9)
- Docker/CI/quality gate (M8); E2E SSE smoke (M7)
- Default MCP surface unchanged (5 tools, 2 resources, 1 prompt)
