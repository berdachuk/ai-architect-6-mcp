# MCP module — agent guide

**Package:** `com.example.medicalmcp.mcp`  
**Modulith deps:** `core`, `medicalcase`, `retrieval`, `embedding`, `dataset`

## Purpose

Spring AI MCP adapters only: `@McpTool`, `@McpResource`, `@McpPrompt`. Delegates to **service interfaces** — never JDBC or `*.impl.*`.

## Surface (production)

| Kind | Count | Classes (planned) |
|---|---:|---|
| Tools | 5 | `MedicalCaseTools` |
| Resources | 2 | `MedicalCaseResources` |
| Prompts | 1 | `MedicalCasePrompts` (`case-analysis`) |
| Completions | 0 | **Forbidden** |

Spec: `docs/01-requirements.md` §6 · Sketches: `docs/03-design.md`

## Constraints

- Map tool args to dataset fields only — no invented columns
- `specialty` filter uses **exact** 13 HF labels
- Prompt `focus`: `description` \| `transcription` \| `keywords` \| `specialty` \| `all`
- No business logic beyond validation + DTO mapping

## Skills

- `.agents/skills/core-architecture/SKILL.md`
- `.agents/skills/testing/SKILL.md`
- `.agents/skills/security-check/SKILL.md` — MCP input validation, no secret leakage in tool responses

## Tests (M5)

- `McpToolsContractIntegrationTest`
- `McpResourcesIntegrationTest`
- Prompt structure tests only (not LLM output quality)
