# Registry schema

Append-only JSONL registries under `registry/` are the **single source of truth** for stable IDs. One JSON object per line. **Never edit an existing line** — append only.

## Files

| File | ID kind | Example |
|---|---|---|
| `req.jsonl` | `REQ-###` functional requirement | `{"id":"REQ-001","summary":"...","section":"01-requirements.md#2","module":"medicalcase","domain_models":["MedicalCase"]}` |
| `nfr.jsonl` | `NFR-###` non-functional requirement | `{"id":"NFR-001","metric":"search_cases p99","target":"< 100 ms","section":"01-requirements.md#8"}` |
| `scn.jsonl` | `SCN-###` behavior scenario | `{"id":"SCN-001","feature":"Case FTS search","req_ids":["REQ-006"],"module":"retrieval"}` |
| `test.jsonl` | `TEST-###` test artifact | `{"id":"TEST-001","class":"ModulithArchitectureTest","module":"core","profile":"default"}` |
| `dec.jsonl` | `DEC-###` decision (index row) | `{"id":"DEC-001","title":"...","status":"Accepted","date":"2026-06-16","modules":["all"],"body":"records/decisions/DEC-001.md"}` |
| `risk.jsonl` | `RISK-###` known risk | `{"id":"RISK-001","summary":"...","severity":"Medium","mitigation":"..."}` |
| `task.jsonl` | `TASK-###` plan task | `{"id":"TASK-001","plan":"M-16","summary":"...","status":"pending"}` |

## Field conventions

- `id` — stable, zero-padded, never reused. `DEC-###` only; legacy `D-###` are immutable aliases (not edited).
- `section` — repo-relative link target (no leading `docs/` prefix needed; paths are repo-relative).
- `module` — owning Modulith package: `core` | `medicalcase` | `embedding` | `retrieval` | `dataset` | `mcp` | `system` | `all`.
- `domain_models` — array of record names owned by `medicalcase` (empty array if none).
- `body` (dec only) — repo-relative path to `records/decisions/DEC-###.md` long-form file.
- `status` — `Accepted` | `Proposed` | `Deprecated` | `Superseded by DEC-###`.

## Minting a new ID

1. Read the registry file.
2. Take `max(existing numeric suffix) + 1`, zero-padded to 3 digits.
3. Append exactly one JSON line. Validate JSON before commit.
4. If a merge conflict occurs on the last line: re-read, recompute `max+1`, append again.

## Migration note (2026-06-21)

The previous single-writer `decisions.md` carried DEC-001..DEC-011 as inline blocks. Those were migrated to `registry/dec.jsonl` (index) + `records/decisions/DEC-###.md` (body) on this date. Existing IDs are preserved; no renumbering.