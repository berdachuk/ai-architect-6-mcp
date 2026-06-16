# dataset module — agent guide

**Package:** `com.example.medicalmcp.dataset`  
**Modulith deps:** `core`, `medicalcase`, `embedding`

## Purpose

One-time CSV ingest: `DatasetLoaderService` — two-pass load (rows → embeddings).

## Load flow

1. **Pass 1:** Read `medical_cases_{train,validation,test}.csv` → insert rows via `MedicalCaseRepository`
2. **Pass 2:** Batch embed via `EmbeddingService` → update `embedding` column
3. **Idempotency:** skip if `COUNT(*) > 0`

Spec: `docs/01-requirements.md` §7

## Constraints

- HF columns only — assign UUID and `split` server-side
- Do not expose loader as MCP tool (startup `CommandLineRunner` or profile)
- No MCP or HTTP surface in this module

## Skills

- `.agents/skills/db-migrations/SKILL.md`
- `.agents/skills/testing/SKILL.md`

## Tests (M2, M4)

- `DatasetLoaderIntegrationTest` (train CSV sample)
- Idempotency: second run does not duplicate rows
