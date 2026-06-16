# M-02 — Modulith foundation (M1)

**Status:** ✅ Archived  
**Date:** 2026-06-16  
**Milestone:** M1

## Objective

Maven scaffold, Flyway `V1__init_medical_cases.sql`, domain records, Spring Modulith `package-info.java` per module, Boot application stub, and foundation tests.

## Deliverables

| Artifact | Path | Status |
|---|---|---|
| Parent POM | `pom.xml` | ✅ |
| Boot entry | `MedicalMcpApplication.java` | ✅ |
| Flyway migration | `V1__init_medical_cases.sql` | ✅ |
| Domain records | `medicalcase/domain/*.java` (5) | ✅ |
| Modulith boundaries | 7× `package-info.java` | ✅ |
| Config | `application.yml`, `application-test.yml` | ✅ |
| Tests | `ModulithArchitectureTest`, `FlywaySchemaIntegrationTest` | ✅ |

## Verification

- `mvn test` — Modulith `verify()` passes
- `mvn verify -Pintegration` — Flyway IT on `pgvector/pgvector:pg17`

## Next

See [00-index.md](../00-index.md) — [M-03 dataset loader](M-03-dataset-loader.md) (requirements M2).
