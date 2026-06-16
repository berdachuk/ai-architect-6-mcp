# M-10 — Prompt lab (M9, optional)

**Status:** ✅ Complete (archived 2026-06-16)  
**Date:** 2026-06-16  
**Milestone:** M9 — [docs/01-requirements.md §18](../../docs/01-requirements.md#18-future-scope-optional)

## Objective

Add optional `promptlab` module for meta-prompting, template evaluation, and promotion workflow for `case-analysis` improvements.

## Deliverables

| Artifact | Path |
|---|---|
| Module scaffold | `promptlab/` — domain, normalization, eval, templates, `@Profile("prompt-lab")` config |
| Eval harness | `SpecialtyClassificationEvaluator` + offline simulator |
| Maven profile | `prompt-lab` → `PromptLabOfflineEvalIntegrationTest` |
| Quality metrics | `PromptLabQualityReporter` merges into `quality-report.json` |
| Promotion path | `promptlab/PROMOTION.md` → M-11 |

## Acceptance criteria

- [x] `mvn verify -Pprompt-lab` runs offline eval without breaking default MCP surface
- [x] Metrics reported alongside retrieval quality report (`promptLab` section)
- [x] No new MCP tools/completions in default profile

## References

- [docs/future/prompt-lab.md](../../docs/future/prompt-lab.md)

## Next

[M-11 prompt integration](../M-11-prompt-integration.md) (requirements M10).
