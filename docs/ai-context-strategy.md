# AI context strategy

**Version:** 2.0  
**Date:** 2026-06-21  
**Project:** `medical-mcp-server`

How AI coding agents should use repository context alongside canonical human docs. This version reflects the **multi-agent-safe** memory bank (DEC-012): append-only registries, per-record files, generated indexes, module locks.

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
│  .agents/memory-bank/                                  │
│    ├─ reference files   (hand-edited, low-frequency)    │
│    ├─ generated indexes  (read-only to agents)          │
│    ├─ registry/*.jsonl   (append-only stable IDs)       │
│    ├─ records/**         (one file per record)          │
│    ├─ locks/<module>.md  (coupled-file-pair ownership)  │
│    └─ worktrees/<slug>/  (per-branch scratchpad)        │
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

**Rule:** `docs/` holds authoritative specifications. The memory bank holds **distilled operational state** sourced from append-only registries and per-record files. Skills hold **how-to workflows**. `AGENTS.md` files hold **boundaries and pointers** — not full specs.

## Multi-agent conflict prevention

The memory bank is partitioned so that **parallel agents in separate git worktrees never edit the same file**:

| Tier | Write pattern | Conflict risk |
|---|---|---|
| Reference files | Hand-edited, rare | Low |
| Generated indexes | Regenerated deterministically by `sync-memory-index.sh` from registries + records. Two agents produce identical output → no merge conflict. | None |
| Append-only registries | One JSON line per ID. Last-line conflict collapses to "who owns the last line" — re-read, recompute `max+1`. | Trivial |
| Per-record files | One file per milestone/decision. Distinct files = zero conflict. | None |
| Module locks | Advisory; turn silent semantic breakages (coupled file pairs) into detectable textual conflicts. | Serialized |
| Worktree scratchpads | Git-ignored; transient per-branch state; never merge to main. | None |

### Principles

- **Append, never rewrite.** Editing an existing registry line breaks ID stability.
- **One record per file.** Never bundle multiple milestones into one record file.
- **Generated indexes are read-only to agents.** `sync-memory-index.sh --check` is a CI gate.
- **Acquire a module lock before editing coupled files** (e.g. prompt template + sanitizer).
- **Use worktree scratchpads for transient state** — promote only `records/` and `registry/` on merge.
- **Serialize, don't race.** If two agents need the same module, the second waits or coordinates.

## Analysis → structure mapping

Module boundaries were derived from [docs/02-architecture.md](02-architecture.md), [03-design.md](03-design.md), and [01-requirements.md](01-requirements.md):

| Module | Type | Nested AGENTS | Domain models owned |
|---|---|---|---|
| `mcp` | Edge (integration) | ✅ | — |
| `medicalcase` | Core domain + persistence API | ✅ | `MedicalCase`, `CaseSummary`, `SemanticMatch`, `SpecialtyCount`, `DatasetStats` |
| `embedding` | Infrastructure service | ✅ | — |
| `retrieval` | Application service | ✅ | — |
| `dataset` | Application service (batch) | ✅ | — |
| `core` | Shared kernel | (rules in root AGENTS only) | — |
| `system` | Ops/health | (thin — no nested file) | — |

## Session workflow

### Start of substantial task

1. `.agents/memory-bank/projectbrief.md`
2. `.agents/memory-bank/activeContext.md` (generated — current focus, risks, open scenarios)
3. `.agents/memory-bank/systemPatterns.md`
4. `.agents/memory-bank/techContext.md`
5. Root `AGENTS.md`
6. Nearest nested `AGENTS.md` for target package
7. Relevant `docs/01`–`05` sections
8. `records/active/M{NN}.md` for the active milestone (if any)

### During implementation

- Load skills per root AGENTS index (TDD → `testing`; schema → `db-migrations`; etc.)
- TDD + security pre/post checks mandatory
- Preserve Modulith boundaries
- Acquire `locks/<module>.md` before editing coupled file pairs

### End of task

- Create/update `records/active/M{NN}.md` (or move to `records/progress/` if complete); release the module lock
- Append `DEC-###`/`REQ-###`/`SCN-###`/`TEST-###`/`RISK-###`/`TASK-###` rows to the matching `registry/*.jsonl` (one line each)
- Update `systemPatterns.md` (reference) if architecture changed; append `DEC-###` to `registry/dec.jsonl` + `records/decisions/DEC-###.md`
- Run `scripts/sync-memory-index.sh` to regenerate index files
- Sync `docs/` if canonical specs changed; note mismatches in `records/active/M{NN}.md`

## Adding skills

1. Create `.agents/skills/<name>/SKILL.md` using standard sections: Description, When to use, Instructions, Boundaries
2. Add row to root `AGENTS.md` Skills index
3. Reference from nested `AGENTS.md` only where module-specific
4. Do **not** duplicate skill body in `AGENTS.md`

## Updating skills

- Edit the `SKILL.md` in place (skills are not append-only; they are curated how-to docs).
- Keep instructions aligned with discovered module reality; if a rule no longer prevents a real failure, delete it (see `write-less-code` skill: "Audit Your Context Files").

## Maintaining the memory bank

| Event | Update |
|---|---|
| Architecture change | `systemPatterns.md` (reference) + append `DEC-###` to `registry/dec.jsonl` + `records/decisions/DEC-###.md` |
| Stack/tooling change | `techContext.md` (reference) |
| Task focus change | Create/update `records/active/M{NN}.md` |
| Completed milestone | Move `records/active/M{NN}.md` → `records/progress/M{NN}.md` |
| New requirement/scenario/test/risk/task | Append one line to the matching `registry/*.jsonl` |
| Doc/code drift | Flag in `records/active/M{NN}.md` + `registry/risk.jsonl` |
| **After any registry/record change** | Run `scripts/sync-memory-index.sh`; `--check` in CI |

**Never store:** secrets, large code dumps, chat logs, unverified architecture, duplicated canonical docs.

## Traceability

- Requirements: `registry/req.jsonl` (`REQ-###`) sourced from `docs/01-requirements.md`
- Non-functional: `registry/nfr.jsonl` (`NFR-###`)
- Scenarios: `registry/scn.jsonl` (`SCN-###`) — Gherkin when BDD adopted (skill: `bdd-traceability`)
- Tests: `registry/test.jsonl` (`TEST-###`) — class#method
- Decisions: `registry/dec.jsonl` (`DEC-###`) index + `records/decisions/DEC-###.md` body
- Risks: `registry/risk.jsonl` (`RISK-###`)
- Tasks: `registry/task.jsonl` (`TASK-###`)
- Generated traceability tables: [productContext.md](../.agents/memory-bank/productContext.md)

## Optional IDE adapters

- **Cursor:** may add `.cursor/rules` that point to `AGENTS.md` and skills — do not fork skill content
- **MCP:** runtime tooling separate from this documentation MCP server product
- Adapters are generated views; canonical sources remain `.agents/skills` and `.agents/memory-bank`

## Anti-patterns

- Duplicating `docs/01-requirements` into `AGENTS.md`
- Nested `AGENTS.md` repeating root global rules
- Hand-editing a generated index file instead of editing the source registry/record and re-running `sync-memory-index.sh`
- Editing an existing registry line instead of appending (breaks ID stability)
- Bundling multiple milestones into one record file
- Editing coupled files without holding the module lock
- Merging a worktree scratchpad into the main branch
- Stale traceability in registries after refactor
- Requirement IDs in docs but not in `registry/req.jsonl` or tests
- BDD scenarios that mirror screens instead of business behavior

## Related

- [AGENTS.md](../AGENTS.md) — root agent index
- [docs/README.md](README.md) — human documentation pipeline
- [DEC-012](../.agents/memory-bank/records/decisions/DEC-012.md) — multi-agent-safe memory bank decision