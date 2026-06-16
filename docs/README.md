# Documentation

**Version:** 2.0.0  
**Project:** `medical-mcp-server`  
**Dataset:** [hpe-ai/medical-cases-classification-tutorial](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial)  
**Reference implementation:** [med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce)

Software documentation for project generation, development, and testing. Read in order when scaffolding or implementing from scratch.

## Development pipeline

| Step | Document | Standard | Use for |
|---|---|---|---|
| — | **[01-requirements.md](01-requirements.md)** | SRS | **Source of truth** — what to build: dataset, MCP surface, NFRs, milestones |
| 1 | **[02-architecture.md](02-architecture.md)** | SAD | System context, Modulith modules, stack, design decisions |
| 2 | **[03-design.md](03-design.md)** | SDD | Schema, domain records, service/repository APIs, MCP class sketches |
| 3 | **[04-testing.md](04-testing.md)** | Test plan | Unit/integration/quality tests, CSV split discipline, CI gates |
| 4 | **[05-deployment.md](05-deployment.md)** | Ops guide | `application.yml`, env vars, Docker, MCP client config |

## Supplementary

| Document | Use for |
|---|---|
| [use-cases.md](use-cases.md) | Actors, workflows, per-tool scenarios, out-of-scope list |
| [ai-context-strategy.md](ai-context-strategy.md) | AI agent context layers (skills, memory bank) |
| [../AGENTS.md](../AGENTS.md) | Root agent index (repo root) |
| [future/prompt-lab.md](future/prompt-lab.md) | Optional M9/M10 — prompt evaluation lab (not required for M1–M8) |

## Quick reference

| Topic | Section |
|---|---|
| Dataset schema & 13 specialties | [01-requirements §2](01-requirements.md#2-source-dataset) |
| MCP tools, resources, prompts | [01-requirements §6](01-requirements.md#6-mcp-surface) |
| Architecture requirements | [01-requirements §9](01-requirements.md#9-architecture-spring-modulith) |
| Development milestones M1–M8 | [01-requirements §14](01-requirements.md#14-milestones) |
| Testing & quality gates | [01-requirements §17](01-requirements.md#17-testing--quality-assurance) · [04-testing](04-testing.md) |
| Environment variables | [01-requirements §13](01-requirements.md#13-environment-variables) · [05-deployment](05-deployment.md) |
| Future prompt-lab (M9/M10) | [01-requirements §18](01-requirements.md#18-future-scope-optional) |

## For AI / codegen agents

Start with [AGENTS.md](../AGENTS.md) and [.agents/memory-bank/](../.agents/memory-bank/).

1. Read **01-requirements** — constrain scope to dataset-backed MCP surface only.
2. Read **02-architecture** — single Maven module, Spring Modulith package boundaries, JDBC only.
3. Implement from **03-design** — Flyway schema, interface/impl services, MCP adapters.
4. Verify with **04-testing** — `ModulithArchitectureTest`, Testcontainers IT, test-split quality gates.
5. Ship with **05-deployment** — Docker Compose, Ollama embeddings, SSE on `:8092`.
