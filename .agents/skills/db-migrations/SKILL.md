# DB Migrations

## Description

Flyway migrations and PostgreSQL/pgvector schema for `medical_case` and extensions.

## When to use

- Creating or altering `src/main/resources/db/migration/V*.sql`
- Adding indexes (HNSW, GIN/tsvector, specialty)
- Reviewing vector dimension or FTS column changes

## Instructions

- Flyway naming: `V{n}__description.sql`
- Initial migration: `V1__init_medical_cases.sql` per `docs/03-design.md`
- Enable `vector` and `pg_trgm` extensions
- Embedding column: `VECTOR(768)` — HNSW cosine index
- Generated `fts` tsvector — do not require app to maintain manually if DB-generated
- Test with `FlywaySchemaIntegrationTest` + Testcontainers `pgvector/pg17`
- Idempotent loader guard is application-level (`COUNT(*) > 0`), not a migration flag table

## Boundaries

- Do not use Hibernate ddl-auto or JPA entities for schema
- Do not change vector dimensions without ADR and re-embed plan
- Do not store secrets in migration files
