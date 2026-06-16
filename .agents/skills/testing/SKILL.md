# Testing

## Description

TDD workflow, test layout, Maven profiles, and retrieval quality gates for `medical-mcp-server`.

## When to use

- Writing any test before implementation (mandatory)
- Setting up Testcontainers, Modulith, or MCP contract tests
- Configuring `integration` / `quality` / `e2e` profiles

## Instructions

### TDD workflow (mandatory)

1. Write test first
2. Requirement alignment review (requirement ID, module, domain models, business outcome)
3. Security pre-check for risky areas
4. Implement minimal code
5. `mvn test` / `mvn verify -Pintegration`
6. Security post-check before commit

### Layout (planned)

```text
src/test/java/com/example/medicalmcp/
├── ModulithArchitectureTest.java
├── integration/
└── quality/
```

### Split discipline

| Split | Role |
|---|---|
| train | Fixtures, loader smoke |
| validation | Tune thresholds |
| test | **Gate** quality metrics — never tune on test |

Detail: `docs/04-testing.md`

### Java Cucumber rule (when adopted)

- `.feature` files in acceptance-test layer, not in domain packages
- Tags: `@req-123`, `@retrieval`, `@mcp`
- Thin step definitions — behavior in services
- One dominant requirement per scenario

## Boundaries

- Do not assert LLM output quality for `case-analysis` prompts
- Do not skip Modulith test when adding modules
- Do not tune quality thresholds on the test split
