# Documentation index

| Document | Audience | Contents |
|---|---|---|
| [PRD.md](PRD.md) | Product / stakeholders | **Source of truth** — requirements, MCP, NFRs, milestones M1–M10 |
| [PLAN.md](PLAN.md) | Implementers | Modulith layout, class sketches, SQL, Docker, security |
| [USE_CASES.md](USE_CASES.md) | All audiences | Actors, workflows, per-tool scenarios, out-of-scope list |
| [TESTING.md](TESTING.md) | Implementers / QA | Test pyramid, quality metrics, split usage (train/validation/test) |
| [PROMPT_IMPROVEMENT.md](PROMPT_IMPROVEMENT.md) | Architects / ML engineers | Optional `prompt-lab` profile — PRD §18, M9/M10 |

**Version:** 1.6.0  
**Dataset:** [hpe-ai/medical-cases-classification-tutorial](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial) — 2,464 rows, 5 CSV columns, 13 specialties  
**Reference pattern:** [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce)

## Quick links

- [Dataset schema & specialties (verified)](PRD.md#2-source-dataset)
- [MCP surface (dataset-compatible)](PRD.md#6-mcp-surface)
- [Use cases catalog](USE_CASES.md)
- [Testing & response quality](TESTING.md)
- [Prompt auto-improvement (M9/M10)](PROMPT_IMPROVEMENT.md)
- [PRD §14 milestones](PRD.md#14-milestones)
- [PRD §17 testing & quality](PRD.md#17-testing--quality-assurance)
- [PRD §18 future prompt-lab](PRD.md#18-future-scope-optional)
- [Spring Modulith architecture](PRD.md#9-architecture-spring-modulith)
- [Data loading (CSV)](PRD.md#7-data-loading)
- [Environment variables](PRD.md#13-environment-variables)
- [Implementation milestones](PLAN.md#milestones)
