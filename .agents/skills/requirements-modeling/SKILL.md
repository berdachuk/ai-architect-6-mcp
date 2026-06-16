# Requirements Modeling

## Description

Stable requirement IDs, vocabulary, and traceability from SRS sections to Modulith modules, tests, and implementation artifacts.

## When to use

- Starting M1+ implementation from `docs/01-requirements.md`
- Adding MCP tools, NFRs, or milestones
- Linking tests/scenarios to requirements

## Instructions

- Canonical SRS: `docs/01-requirements.md` (sections §1–§18)
- Assign IDs when implementing:
  - `REQ-###` — functional (map from §6 MCP, §7 loading, etc.)
  - `NFR-###` — from §8
  - `TEST-###` — test classes or `@Tag` groups
  - `DEC-###` — log in `.agents/memory-bank/decisions.md`
- Each `REQ` should record: owning module, domain models, test artifacts
- Reuse section numbers in commit messages until formal IDs exist (e.g. `REQ-006` ↔ §6)
- Update `systemPatterns.md` traceability table when mappings change

## Boundaries

- Do not contradict SRS without human approval and doc update
- Do not invent requirements for prompt-lab (M9/M10) in production MCP scope
- Do not mark traceability complete without verifying links in repo files
