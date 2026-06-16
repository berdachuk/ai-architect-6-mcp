# BDD Traceability

## Description

Preserve explicit links between functional requirements, domain language, Gherkin scenarios, step definitions, and implementation artifacts.

## When to use

- New or changed functional requirements
- New or updated Gherkin feature files
- Review of acceptance coverage
- TDD tasks that require executable business specifications
- Refactoring that may break requirement-to-test traceability

## Instructions

- Start from business behavior, not UI or endpoint mechanics
- Reuse existing requirement IDs or create stable new ones (`REQ-###`, `SCN-###`)
- Identify owning modules and affected domain models
- Write Gherkin using project domain vocabulary (13 specialties, dataset fields)
- Keep each scenario focused on a single business outcome
- Tag scenarios: `@req-006 @mcp @search`
- Map scenarios to step definitions and `TEST-###` artifacts
- Record traceability in plans or `activeContext.md`
- Flag ambiguities in `activeContext.md` when canonical docs are unclear

### Example

```gherkin
@req-006 @mcp @search
Feature: Case full-text search

  Scenario: SCN-010 User searches cases by clinical terms
    Given the dataset is loaded
    When a client calls search_cases with query "chest pain"
    Then results contain cases whose FTS matches the query
```

## Boundaries

- Do not invent requirements
- Do not merge unrelated requirements into one scenario
- Do not treat BDD prose as authoritative when code and approved docs contradict it
- Do not mark traceability complete unless links were actually checked
