# embedding module — agent guide

**Package:** `com.example.medicalmcp.embedding`  
**Modulith deps:** `core` only

## Purpose

Embedding transport: mandatory `EmbeddingEndpointPool`, `EmbeddingService` API + impl, manual `OpenAiEmbeddingModel` per endpoint.

## Key components (planned)

- `EmbeddingEndpointPool`, `EndpointState`
- `EmbeddingEndpointPoolConfig`, `EmbeddingMultiEndpointProperties`
- `EmbeddingService` / `EmbeddingServiceImpl`

## Constraints

- **No** Spring AI embedding auto-config — exclude `OpenAiEmbeddingAutoConfiguration`
- Model: `nomic-embed-text:v1.5`, **768** dimensions only
- ≥1 endpoint required — fail fast at startup if empty
- Embed input text: `{sampleName}. {description} {keywords}` (not full transcription)
- Do not depend on `medicalcase` repository types

Spec: `docs/01-requirements.md` §4

## Skills

- `.agents/skills/core-architecture/SKILL.md`
- `.agents/skills/security-check/SKILL.md` — API keys, endpoint URLs

## Tests (M4)

- Pool failover / skip behavior
- `EmbeddingService` integration with Testcontainers or mock endpoint
- Loader pass 2 coordination with `dataset` module
