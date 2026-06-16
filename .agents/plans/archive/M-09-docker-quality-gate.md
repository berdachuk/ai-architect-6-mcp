# M-09 — Docker + quality gate (M8)

**Status:** ✅ Complete (archived 2026-06-16)  
**Date:** 2026-06-16  
**Milestone:** M8 — [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones)

## Objective

Ship Docker Compose stack, CI quality profile, and full **test**-split retrieval benchmarks.

## Prerequisites

- [M-08](M-08-e2e-smoke.md) complete — E2E smoke validated

## Deliverables

| Artifact | Path |
|---|---|
| Docker | `Dockerfile`, `docker-compose.yml`, `.dockerignore` |
| CI workflow | `.github/workflows/ci.yml` — `mvn test`, `mvn verify -Pintegration` |
| Quality workflow | `.github/workflows/quality.yml` — nightly `mvn verify -Pquality` |
| Quality profile | `quality` Maven profile — `RetrievalQualityGateIntegrationTest` on test-split fixture |
| Quality report | `target/test-output/quality-report.json` via `QualityReportWriter` |
| Docs | README quick start (Docker Compose), Maven profiles table |

## Requirement traceability

| ID | Summary | Module | Test |
|---|---|---|---|
| NFR-001 | Docker deployment | ops | `docker compose` + actuator health |
| REQ-006 | Retrieval quality gates | `retrieval` | `RetrievalQualityGateIntegrationTest` |
| NFR-003 | CI automation | — | GitHub Actions |

## Acceptance criteria

- [x] `docker compose up` serves MCP on `:8092/sse` with healthcheck (Ollama on host)
- [x] PR CI runs unit + integration tests
- [x] Quality profile meets gates on test-split fixture (`test-sample-10.csv`); nightly workflow uploads report
- [x] Full 2464-row load via Docker default `application.yml` sources (staging)

## References

- [docs/01-requirements.md §15](../../docs/01-requirements.md#15-docker-compose)
- [docs/05-deployment.md](../../docs/05-deployment.md)
- [docs/04-testing.md §6–7](../../docs/04-testing.md#6-response-quality-testing)

## Next

[M-10 prompt lab](../M-10-prompt-lab.md) (requirements M9, optional).
