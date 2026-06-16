# M-12 — Meta-prompting lab (M9 extended, optional)

**Status:** ⬜ Active  
**Date:** 2026-06-16  
**Milestone:** M9 extended — [docs/future/prompt-lab.md §9](../../docs/future/prompt-lab.md#9-implementation-phases) P3–P5

## Objective

Complete deferred prompt-lab scope: live LLM evaluation, meta-prompt improvement with failure context, and MCP lab tools under `prompt-lab` profile only.

## Prerequisites

- [M-10](archive/M-10-prompt-lab.md) offline eval harness
- [M-11](archive/M-11-prompt-integration.md) production template promotion

## Deliverables

| Artifact | Path |
|---|---|
| Meta service | `MetaPromptImprovementService` |
| Live eval | Chat-client wired `SpecialtyClassificationEvaluator` (optional path) |
| MCP lab tools | `PromptLabTools` — evaluate / improve / compare / gate |
| Config | `application-prompt-lab.yml` extensions |
| CI | Optional nightly `mvn verify -Pprompt-lab` workflow |

## Acceptance criteria

- [ ] `evaluate_specialty_prompt`, `compare_specialty_prompts` work under `prompt-lab` profile
- [ ] Meta-improvement includes failure-context examples from prior eval run
- [ ] Default profile MCP surface unchanged (5 tools, 2 resources, 1 prompt)
- [ ] `mvn verify -Pprompt-lab` passes with live or stubbed chat client

## References

- [docs/future/prompt-lab.md](../../docs/future/prompt-lab.md)
- [PRACTICAL_TASK_REPORT](https://github.com/berdachuk/ai-architect-6-tasks/blob/main/specialty-classification-reasoning/PRACTICAL_TASK_REPORT.md)

## Out of scope

- Production specialty classification API
- New default-profile MCP tools
