# M-12 — Meta-prompting lab (M9 extended, optional)

**Status:** ✅ Complete (archived 2026-06-16)  
**Date:** 2026-06-16  
**Milestone:** M9 extended — [docs/future/prompt-lab.md §9](../../docs/future/prompt-lab.md#9-implementation-phases) P3–P5

## Objective

Complete deferred prompt-lab scope: meta-prompt improvement with failure context and MCP lab tools under `prompt-lab` profile only.

## Deliverables

| Artifact | Path |
|---|---|
| Meta service | `MetaPromptImprovementService` |
| Eval orchestration | `SpecialtyPromptClassificationService`, `PromptLabEvalRunStore` |
| MCP lab tools | `PromptLabTools` — evaluate / improve / compare / gate / list |
| Stub classifier | `OfflinePromptLabClassificationClient` |
| Config | Extended `PromptLabProperties`, `application-prompt-lab.yml` |
| CI | `prompt-lab` job in `.github/workflows/ci.yml` |
| Tests | `PromptLabToolsIntegrationTest`, `MetaPromptImprovementServiceTest` |

## Acceptance criteria

- [x] `evaluate_specialty_prompt`, `compare_specialty_prompts` work under `prompt-lab` profile
- [x] Meta-improvement includes failure-context examples from prior eval run
- [x] Default profile MCP surface unchanged (5 tools, 2 resources, 1 prompt)
- [x] `mvn verify -Pprompt-lab` passes with stubbed classifier

## Deferred to M-15

- Live `ChatClient` / Ollama chat wiring (`medicalmcp.prompt-lab.chat.enabled`)
- Full test-split `PromptLabGateTest` in quality profile

## References

- [docs/future/prompt-lab.md](../../docs/future/prompt-lab.md)
