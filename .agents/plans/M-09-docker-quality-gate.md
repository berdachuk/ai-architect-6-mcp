# M-09 — Docker + quality gate (M8)

**Status:** ⬜ Planned  
**Date:** 2026-06-16  
**Milestone:** M8 — [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones)

## Objective

Ship Docker Compose stack, CI quality profile, and full **test**-split retrieval benchmarks.

## Prerequisites

- [M-08](M-08-e2e-smoke.md) complete — E2E smoke validated
- Full dataset + embeddings loadable in CI/nightly env

## Deliverables

| Artifact | Path |
|---|---|
| Docker | `Dockerfile`, `docker-compose.yml` — PG + server; Ollama on host |
| CI workflow | GitHub Actions: `mvn test`, `mvn verify -Pintegration` (Linux or WSL runner) |
| Quality profile | `quality` Maven profile — `FtsRetrievalQualityIntegrationTest`, `SemanticRetrievalQualityIntegrationTest` on **test** split |
| Quality report | `target/test-output/quality-report.json` per [docs/04-testing.md §7](../../docs/04-testing.md) |
| Docs | Finalize deployment guide, README quick start |

## Acceptance criteria

- [ ] `docker compose up` healthy; MCP on `:8092/sse`
- [ ] PR CI runs unit + integration tests
- [ ] Nightly quality gate meets initial thresholds on **test** split (tuned on validation)
- [ ] `get_dataset_stats` → 2464 rows, 13 specialties with full load

## References

- [docs/01-requirements.md §15](../../docs/01-requirements.md#15-docker-compose)
- [docs/05-deployment.md](../../docs/05-deployment.md)
- [docs/04-testing.md §6](../../docs/04-testing.md)
