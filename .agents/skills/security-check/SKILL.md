---
name: security-check
description: Security review agent for AI-generated and human-written code
version: "1.0"
tags:
  - security
  - owasp
  - auth
  - secrets
  - dependencies
---

# Security Check

## Description

OWASP-aligned review for auth, secrets, injection, dependencies, and MCP/infra configuration. Use **before** risky implementation and **after** before commit.

## When to use

- Auth, security config, `SecurityConfig`
- SQL/JDBC, user-controlled search queries, MCP tool inputs
- New dependencies or Docker/env configuration
- Embedding API keys, database credentials

## Instructions

### Before implementation

- Identify trust boundaries (MCP clients, DB, Ollama endpoints)
- List inputs requiring validation (`search_cases` query, UUID params, limits)
- Check for secrets in planned config — use env vars only
- Note dependency supply-chain risks for new libraries

### Review checklist

| Area | Check |
|---|---|
| Secrets | No keys/passwords in repo; `.env` gitignored |
| Auth | Default permit-all documented; prod JWT wiring not half-done |
| Injection | Parameterized JDBC; sanitize/limit FTS query length |
| MCP | Tool args validated; no stack traces in tool responses |
| Dependencies | Prefer BOM-managed versions; audit new deps |
| Infra | Docker not exposing DB publicly; health endpoints scoped |
| CI | No secrets in workflow logs |

### OWASP quick pass

- A01 Broken Access Control — MCP exposure scope
- A03 Injection — SQL/FTS inputs
- A05 Security Misconfiguration — `application.yml`, Actuator
- A06 Vulnerable Components — Maven deps
- A07 Auth failures — SSE endpoint exposure

### Blocking criteria

**Escalate to human** (do not auto-fix, do not approve):

- Critical/high: committed secrets, SQL concatenation, auth bypass
- Medium: missing input limits, verbose error leakage to MCP clients

## Boundaries

- **Do not auto-fix vulnerabilities**
- **Do not approve PRs**
- Report findings; human decides remediation
- Run before implementation on risky tasks and after implementation before commit
