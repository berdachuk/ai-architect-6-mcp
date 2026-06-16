# Core Architecture

## Description

Spring Modulith package boundaries, module dependency rules, and interface/impl layering for `medical-mcp-server`. Covers all packages under `com.example.medicalmcp`.

## When to use

- Adding or moving classes between modules
- Creating new `package-info.java` or changing `allowedDependencies`
- Reviewing whether a dependency violates Modulith rules
- Scaffolding M1 or refactoring cross-module calls

## Instructions

- Single Maven module; boundaries via `@ApplicationModule` on each package
- Dependency graph is fixed unless `docs/02-architecture.md` and memory bank are updated:
  - `mcp` → `core`, `medicalcase`, `retrieval`, `embedding`, `dataset`
  - `dataset` / `retrieval` → `core`, `medicalcase`, `embedding`
  - `embedding` / `medicalcase` → `core` only
- Public contracts live in `service/` and `repository/`; JDBC in `impl/`
- MCP module is an edge adapter — no persistence code
- Run `ModulithArchitectureTest` after any boundary change
- Mirror patterns from [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce)

## Boundaries

- Do not introduce JPA, reactive stack, or extra Maven modules without ADR in `decisions.md`
- Do not let `medicalcase` depend on feature modules
- Do not reference `*.impl.*` from `mcp` or sibling modules
