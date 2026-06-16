# M-11 — Prompt integration (M10, optional)

**Status:** ⬜ Active  
**Date:** 2026-06-16  
**Milestone:** M10 — [docs/01-requirements.md §18](../../docs/01-requirements.md#18-future-scope-optional)

## Objective

Promote the winning `case-analysis` template from prompt-lab eval into `MedicalCasePrompts` with regression tests.

## Prerequisites

- [M-10](archive/M-10-prompt-lab.md) complete — offline eval harness and `prompt-lab` profile

## Deliverables

| Artifact | Path |
|---|---|
| Production prompt | Updated `MedicalCasePrompts.java` |
| Regression tests | Prompt structure IT (no LLM quality assertions) |
| Docs | Traceability in requirements / use-cases |

## Acceptance criteria

- [ ] `case-analysis` prompt matches promoted template for all focus modes
- [ ] MCP contract tests unchanged for tool/resource surface
- [ ] `mvn verify -Pintegration` passes

## References

- [docs/future/prompt-lab.md §9](../../docs/future/prompt-lab.md#9-implementation-phases)
