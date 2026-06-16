# Decisions

ADR-style log. Full rationale in linked docs where applicable.

## DEC-001 — Single-module Spring Modulith

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | all |
| Rationale | Match `med-expert-match-ce`; package boundaries + `verify()` in CI |
| Source | [docs/02-architecture.md](../docs/02-architecture.md) |

## DEC-002 — JDBC only, no JPA

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | `medicalcase`, `retrieval`, `dataset` |
| Rationale | Consistency with reference project; avoid Hibernate schema drift |
| Source | [docs/02-architecture.md](../docs/02-architecture.md) |

## DEC-003 — Mandatory EmbeddingEndpointPool

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | `embedding`, `dataset` |
| Rationale | All embed paths through pool; ≥1 endpoint at startup |
| Source | [docs/01-requirements.md §4](../docs/01-requirements.md#4-embedding-model) |

## DEC-004 — MCP surface: 5 tools, 0 completions

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | `mcp` |
| Rationale | Dataset fidelity; UUID identity; `sample_name` not unique |
| Source | [docs/01-requirements.md §6](../docs/01-requirements.md#6-mcp-surface) |

## DEC-005 — Docs pipeline v2.0.0

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Rationale | IEEE-style flat layout for codegen and onboarding |
| Source | [docs/README.md](../docs/README.md) |

## DEC-006 — AI context layer in `.agents/`

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Rationale | Tool-agnostic skills + memory bank; root `AGENTS.md` as index |
| Source | [docs/ai-context-strategy.md](../docs/ai-context-strategy.md) |

## DEC-007 — Modulith test without Spring context

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Rationale | `ModulithArchitectureTest` uses `ApplicationModules.verify()` without DB — fast `mvn test` |
| Source | M1 implementation |

## DEC-008 — Modulith named interfaces for medicalcase

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | `medicalcase`, `dataset`, `retrieval` |
| Rationale | `medicalcase :: *` caused Modulith violations; expose `domain` and `repository` via `@NamedInterface` |
| Source | M2 implementation |

## DEC-009 — WSL for Docker on Windows

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Rationale | Docker Desktop exposes the daemon to WSL2; Testcontainers and `docker` CLI fail or misbehave from native Windows shells |
| Source | Dev environment note |

## DEC-010 — External SQL files with `@InjectSql`

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | `core`, `medicalcase`, `retrieval`, `dataset` |
| Rationale | Keep JDBC repository impls readable; SQL lives in `src/main/resources/sql/{module}/` and is loaded at startup — same pattern as [med-expert-match-ce `MedicalCaseRepositoryImpl`](https://github.com/berdachuk/med-expert-match-ce/blob/main/src/main/java/com/berdachuk/medexpertmatch/medicalcase/repository/impl/MedicalCaseRepositoryImpl.java) |
| Convention | `@InjectSql("/sql/medicalcase/findById.sql") String findByIdSql;` on `String` fields in `*/repository/impl/*`; annotation + `SqlInjectBeanPostProcessor` in `core` |
| Source | [med-expert-match-ce `@InjectSql`](https://github.com/berdachuk/med-expert-match-ce/blob/main/src/main/java/com/berdachuk/medexpertmatch/core/repository/sql/InjectSql.java) |

> **Note:** `@InjectSql` applies only to `*/repository/impl/*`. Integration test cleanup uses Spring `@Sql` with scripts from `src/main/resources/sql/{module}/`.

## DEC-011 — Named bind variables in SQL

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-06-16 |
| Modules | `core`, `medicalcase`, `retrieval`, `dataset` |
| Rationale | Named parameters (`:id`, `:specialty`) are self-documenting, reorder-safe, and pair with `NamedParameterJdbcTemplate` / `MapSqlParameterSource` — no positional `?` placeholders |
| Convention | SQL files use `:name` binds only; repository impl passes `MapSqlParameterSource` or `Map<String, Object>` with matching keys |
| Source | Complements [DEC-010](#dec-010--external-sql-files-with-injectsql); aligns with med-expert-match-ce repository style |
