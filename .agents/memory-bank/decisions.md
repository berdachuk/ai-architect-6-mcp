# Decisions

**Generated** by `scripts/sync-memory-index.sh` — do not hand-edit. Index rows from `registry/dec.jsonl`; bodies in `records/decisions/DEC-###.md`.

ADR-style log. Full rationale in linked records/docs where applicable.

## DEC-001 — Single-module Spring Modulith

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | all |
| Rationale | Match med-expert-match-ce; package boundaries + verify() in CI |
| Body | [records/decisions/DEC-001.md](records/decisions/DEC-001.md) |

## DEC-002 — JDBC only, no JPA

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | medicalcase,retrieval,dataset |
| Rationale | Consistency with reference project; avoid Hibernate schema drift |
| Body | [records/decisions/DEC-002.md](records/decisions/DEC-002.md) |

## DEC-003 — Mandatory EmbeddingEndpointPool

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | embedding,dataset |
| Rationale | All embed paths through pool; >=1 endpoint at startup |
| Body | [records/decisions/DEC-003.md](records/decisions/DEC-003.md) |

## DEC-004 — MCP surface: 5 tools, 0 completions

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | mcp |
| Rationale | Dataset fidelity; id identity; sample_name not unique |
| Body | [records/decisions/DEC-004.md](records/decisions/DEC-004.md) |

## DEC-005 — Docs pipeline v2.0.0

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Rationale | IEEE-style flat layout for codegen and onboarding |
| Body | [records/decisions/DEC-005.md](records/decisions/DEC-005.md) |

## DEC-006 — AI context layer in .agents/

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Rationale | Tool-agnostic skills + memory bank; root AGENTS.md as index |
| Body | [records/decisions/DEC-006.md](records/decisions/DEC-006.md) |

## DEC-007 — Modulith test without Spring context

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | core |
| Rationale | ModulithArchitectureTest uses ApplicationModules.verify() without DB — fast mvn test |
| Body | [records/decisions/DEC-007.md](records/decisions/DEC-007.md) |

## DEC-008 — Modulith named interfaces for medicalcase

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | medicalcase,dataset,retrieval |
| Rationale | medicalcase :: * caused Modulith violations; expose domain and repository via @NamedInterface |
| Body | [records/decisions/DEC-008.md](records/decisions/DEC-008.md) |

## DEC-009 — WSL for Docker on Windows

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Rationale | Docker Desktop WSL2 backend; Testcontainers/docker CLI fail or misbehave from native Windows shells |
| Body | [records/decisions/DEC-009.md](records/decisions/DEC-009.md) |

## DEC-010 — External SQL files with @InjectSql

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | core,medicalcase,retrieval,dataset |
| Rationale | Keep JDBC repository impls readable; SQL lives in src/main/resources/sql/{module}/ and is loaded at startup — same pattern as med-expert-match-ce MedicalCaseRepositoryImpl |
| Body | [records/decisions/DEC-010.md](records/decisions/DEC-010.md) |

## DEC-011 — Named bind variables in SQL

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | core,medicalcase,retrieval,dataset |
| Rationale | Named parameters (:id, :specialty) are self-documenting, reorder-safe, and pair with NamedParameterJdbcTemplate / MapSqlParameterSource — no positional ? placeholders |
| Body | [records/decisions/DEC-011.md](records/decisions/DEC-011.md) |

## DEC-012 — Multi-agent-safe memory bank (registries + per-record files + module locks)

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-21 |
| Rationale | Single-writer activeContext.md/progress.md/decisions.md caused merge conflicts under parallel agent work; migrate to append-only JSONL registries, one-file-per-record, generated indexes via sync-memory-index.sh |
| Body | [records/decisions/DEC-012.md](records/decisions/DEC-012.md) |
