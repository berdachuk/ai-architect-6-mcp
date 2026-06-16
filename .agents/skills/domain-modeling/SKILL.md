# Domain Modeling

## Description

Java records, repository contracts, and invariants for the `medicalcase` module and shared DTO shapes used by `retrieval` and `mcp`.

## When to use

- Defining or changing `MedicalCase`, `CaseSummary`, `SemanticMatch`, `SpecialtyCount`, `DatasetStats`
- Designing `MedicalCaseRepository` method signatures
- Validating dataset field mapping (5 HF columns + server columns)

## Instructions

- Use immutable Java records in `medicalcase/domain/`
- UUID `id` is server-assigned — never from CSV
- `sample_name` is display text, not a unique key
- `medical_specialty` must match one of 13 exact HF labels
- `keywords` may be null — handle in mapping and FTS
- Repository returns domain types, not `Map` or raw JDBC rows
- Keep clinical semantics aligned with `docs/01-requirements.md` §2

## Boundaries

- Do not add domain fields not backed by schema or requirements
- Do not put Spring stereotypes on records or repository interfaces
- Do not embed MCP or embedding client types in domain records
