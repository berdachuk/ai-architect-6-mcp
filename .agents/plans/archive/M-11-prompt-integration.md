# M-11 — Prompt integration (M10, optional)

**Status:** ✅ Complete (archived 2026-06-16)  
**Date:** 2026-06-16  
**Milestone:** M10 — [docs/01-requirements.md §18](../../docs/01-requirements.md#18-future-scope-optional)

## Objective

Promote the winning `case-analysis` template from prompt-lab eval into `MedicalCasePrompts` with regression tests.

## Deliverables

| Artifact | Path |
|---|---|
| Shared instructions | `core/prompt/PromotedSpecialtyClassificationInstructions` |
| Production prompt | `MedicalCasePrompts` — `focus=specialty` |
| Regression tests | `MedicalCasePromptsStructureTest`, `McpToolsContractIntegrationTest` |
| Docs | `01-requirements`, `use-cases`, `04-testing` |

## Acceptance criteria

- [x] `case-analysis` prompt matches promoted template for `focus=specialty`
- [x] All focus modes return structured templates (IT)
- [x] MCP contract tests unchanged for tool/resource surface
- [x] `mvn verify -Pintegration` passes

## References

- [docs/future/prompt-lab.md §9](../../docs/future/prompt-lab.md#9-implementation-phases) P6

## Next

Optional extended lab scope: [M-12 meta-prompting lab](../M-12-meta-prompting-lab.md).
