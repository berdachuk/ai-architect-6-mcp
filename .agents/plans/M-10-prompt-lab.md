# M-10 — Prompt lab (M9, optional)

**Status:** ⬜ Active  
**Date:** 2026-06-16  
**Milestone:** M9 — [docs/01-requirements.md §18](../../docs/01-requirements.md#18-future-scope-optional)

## Objective

Add optional `promptlab` module for meta-prompting, template evaluation, and promotion workflow for `case-analysis` improvements.

## Prerequisites

- [M-09](archive/M-09-docker-quality-gate.md) complete — Docker, CI, quality gate in place
- Stable MCP `case-analysis` prompt baseline (M5)

## Deliverables

| Artifact | Path |
|---|---|
| Module scaffold | `promptlab/` package or separate profile |
| Eval harness | Classification prompt metrics on validation split |
| Maven profile | `prompt-lab` per [docs/future/prompt-lab.md](../../docs/future/prompt-lab.md) |
| Promotion path | Document how winning templates feed `MedicalCasePrompts` (M11) |

## Acceptance criteria

- [ ] `mvn verify -Pprompt-lab` runs offline eval without breaking default MCP surface
- [ ] Metrics reported alongside retrieval quality report
- [ ] No new MCP tools/completions in default profile

## References

- [docs/future/prompt-lab.md](../../docs/future/prompt-lab.md)
- [docs/01-requirements.md §18](../../docs/01-requirements.md#18-future-scope-optional)

## Next

[M-11 prompt integration](M-11-prompt-integration.md) (requirements M10) — wire promoted template into production prompt.
