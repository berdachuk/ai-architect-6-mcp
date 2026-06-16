# AI context strategy

**Version:** 1.0  
**Date:** 2026-06-16  
**Project:** `medical-mcp-server`

How AI coding agents should use repository context alongside canonical human docs.

## Layer model

```text
┌─────────────────────────────────────────────────────────┐
│  Optional IDE adapters (.cursor/, MCP, etc.)            │
└───────────────────────────┬─────────────────────────────┘
                            │ reads / generates from
┌───────────────────────────▼─────────────────────────────┐
│  .agents/skills/**/SKILL.md   (capabilities, workflows) │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  .agents/memory-bank/*.md     (session continuity)      │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  Nested AGENTS.md (2–5 module boundaries)               │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  Root AGENTS.md               (compact index, ≤150 lines)│
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  docs/01–05                   (canonical deep specs)      │
└─────────────────────────────────────────────────────────┘
```

**Rule:** `docs/` holds authoritative specifications. Memory bank holds **distilled operational state**. Skills hold **how-to workflows**. `AGENTS.md` files hold **boundaries and pointers** — not full specs.

## Analysis → structure mapping

This repo was analyzed as **docs-only** (no Java yet). Module boundaries were derived from:

- [docs/02-architecture.md](02-architecture.md) — Modulith graph
- [docs/03-design.md](03-design.md) — classes and schema
- [docs/01-requirements.md](01-requirements.md) — MCP surface and dataset

| Module | Type | Nested AGENTS |
|---|---|---|
| `mcp` | Edge (integration) | ✅ |
| `medicalcase` | Core domain + persistence API | ✅ |
| `embedding` | Infrastructure service | ✅ |
| `retrieval` | Application service | ✅ |
| `dataset` | Application service (batch) | ✅ |
| `core` | Shared kernel | (rules in root AGENTS only) |
| `system` | Ops/health | (thin — no nested file) |

## Session workflow

### Start of substantial task

1. `.agents/memory-bank/projectbrief.md`
2. `.agents/memory-bank/activeContext.md`
3. `.agents/memory-bank/systemPatterns.md`
4. `.agents/memory-bank/techContext.md`
5. Root `AGENTS.md`
6. Nearest nested `AGENTS.md` for target package
7. Relevant `docs/01`–`05` sections

### During implementation

- Load skills per root AGENTS index (TDD → `testing`; schema → `db-migrations`; etc.)
- TDD + security pre/post checks mandatory
- Preserve Modulith boundaries

### End of task

- Update `activeContext.md`, append `progress.md`
- Update `decisions.md` / `systemPatterns.md` if architecture changed
- Sync `docs/` if canonical specs changed; note mismatches in `activeContext.md`

## Adding skills

1. Create `.agents/skills/<name>/SKILL.md` using standard sections: Description, When to use, Instructions, Boundaries
2. Add row to root `AGENTS.md` Skills index
3. Reference from nested `AGENTS.md` only where module-specific
4. Do **not** duplicate skill body in `AGENTS.md`

## Maintaining memory bank

| Event | Update |
|---|---|
| Architecture change | `systemPatterns.md`, `decisions.md` |
| Stack/tooling change | `techContext.md` |
| Task focus change | `activeContext.md` |
| Completed work | `progress.md` |
| Doc/code drift | `activeContext.md` (flag for human) |

**Never store:** secrets, large code dumps, chat logs, unverified architecture.

## Traceability

- Requirements: `docs/01-requirements.md` (assign `REQ-###` during implementation)
- Scenarios: `SCN-###` in Gherkin when BDD adopted
- Tests: `TEST-###` or class names linked in plans/progress
- Decisions: `DEC-###` in `decisions.md`
- Skill: `.agents/skills/bdd-traceability/SKILL.md`

## Optional IDE adapters

- **Cursor:** may add `.cursor/rules` that point to `AGENTS.md` and skills — do not fork skill content
- **MCP:** runtime tooling separate from this documentation MCP server product
- Adapters are generated views; canonical sources remain `.agents/skills` and `.agents/memory-bank`

## Anti-patterns

- Duplicating `docs/01-requirements` into `AGENTS.md`
- Nested `AGENTS.md` repeating root global rules
- Stale traceability in memory bank after refactor
- Requirement IDs in docs but not in tests
- BDD scenarios that mirror screens instead of business behavior

## Related

- [AGENTS.md](../AGENTS.md) — root agent index
- [docs/README.md](README.md) — human documentation pipeline
