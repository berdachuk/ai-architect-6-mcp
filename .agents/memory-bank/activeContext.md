# Active context

**Generated** by `scripts/sync-memory-index.sh` — do not hand-edit. Edit `records/active/*.md` and re-run.

## Current focus
- **M-16 — CI combined quality gates (active)** — [record](records/active/M-16.md) · [plan](../plans/M-16-ci-combined-quality-gates.md)

## Active risks
| RISK-001 | Low | Doc/code drift: docs/01-requirements.md §5 schema still shows id UUID DEFAULT gen_random_uuid() but code uses 24-char hex TEXT (M-18) |
| RISK-002 | High | Mixing embedding models with different dimensions corrupts vector search |
| RISK-003 | Medium | sample_name is not unique — clients must use id for case identity |
| RISK-004 | Low | Keywords nullable (~36% empty) — FTS and embedding must handle null |
| RISK-005 | Medium | Quality thresholds tuned on validation must not leak to test split |
| RISK-006 | Medium | Default SecurityConfig permit-all — SSE endpoint exposed without auth in dev |
| RISK-007 | Medium | Parallel agents editing coupled prompt template + sanitizer without lock produce green-but-broken merge |

## Open scenarios (BDD)
- **SCN-001** — Case full-text search
- **SCN-002** — Case retrieval by id
- **SCN-003** — Semantic similarity search
- **SCN-004** — Specialty listing
- **SCN-005** — Dataset statistics
- **SCN-006** — Two-pass idempotent load

## Reference files (hand-edit these)
- [projectbrief.md](projectbrief.md)
- [systemPatterns.md](systemPatterns.md)
- [techContext.md](techContext.md)
- [productContext.md](productContext.md) (prose hand-edited; tables generated)
