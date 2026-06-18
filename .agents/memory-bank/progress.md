# Progress log

## 2026-06-16 — Docs v2.0.0

- Reorganized documentation into numbered pipeline: `01-requirements` → `05-deployment`
- Committed `4caab79` on `develop`

## 2026-06-16 — AI context bootstrap

- Added `.gitignore`, root `AGENTS.md`
- Created `.agents/memory-bank/`, `.agents/skills/`, `.agents/plans/`
- Added nested module `AGENTS.md` (5 packages)
- Added `docs/ai-context-strategy.md`
- **Modules touched:** (docs/agents only)
- **Plan archived:** `.agents/plans/archive/M-01-ai-context-foundation.md`
- **Tests:** none
- **Traceability:** foundation for M1+ implementation

## 2026-06-16 — M1 Modulith foundation

- Added `pom.xml`, `MedicalMcpApplication`, 7 Modulith modules, 5 domain records
- Flyway `V1__init_medical_cases.sql` (pgvector, HNSW, FTS)
- Tests: `ModulithArchitectureTest`, `FlywaySchemaIntegrationTest` — all pass
- **REQ-005, REQ-009** · modules: all · plan archived: `M-02-modulith-foundation.md`

## 2026-06-16 — M2 Dataset loader

- `MedicalCaseRepository` (insert, findById, countAll), `DatasetLoaderService` pass 1
- `train-sample-10.csv` fixture, `DatasetLoaderIntegrationTest`
- Modulith `@NamedInterface` on medicalcase domain/repository packages
- **REQ-005, REQ-007** · plan archived: `M-03-dataset-loader.md`

## 2026-06-16 — M3 Retrieval module

- `MedicalCaseRepository` FTS, `listSpecialties`, `countBySplit`
- `VectorSearchService` + Caffeine stats cache, limit clamp
- IT: repository, stats, `FtsRetrievalQualityIntegrationTest` subset
- Singleton Testcontainers Postgres for stable WSL/Docker IT
- **REQ-006** · plan archived: `M-04-retrieval-module.md`

## 2026-06-16 — M4 Embedding module

- `@InjectSql`, SQL files, `EmbeddingEndpointPool`, `EmbeddingService`, loader pass 2
- `semanticSearch` via pgvector; `SemanticRetrievalQualityIntegrationTest` subset
- **REQ-004, REQ-006, DEC-010, DEC-011** · plan archived: `M-05-embedding-module.md`

## 2026-06-16 — M5 MCP module + SQL/IT alignment

- `MedicalCaseTools`, `MedicalCaseResources`, `MedicalCasePrompts`; `spring-ai-starter-mcp-server-webmvc`
- `McpToolsContractIntegrationTest`, `McpResourcesIntegrationTest`
- SQL/IT: `@Sql` cleanup, `SharedPostgresContainer`, behavioral Flyway IT
- **REQ-006, DEC-004** · plan archived: `M-06-mcp-module.md`

## 2026-06-16 — M6 Config + security

- `RetrievalProperties`, `SecurityConfig`, `JacksonConfig`; actuator + security starters
- Property binding/validation tests; `StatsCacheTtlIntegrationTest`
- MCP resources return JSON strings; invalid UUID guard in tools/prompts
- **NFR-003, NFR-004, REQ-006** · plan archived: `M-07-config-security.md`

## 2026-06-16 — M7 End-to-end smoke

- `McpSseSmokeIntegrationTest` — `McpSyncClient` over SSE (`@RANDOM_PORT`)
- `McpSseTestClientFactory`, `McpSmokeTestSupport`; Maven `e2e` profile + `application-e2e.yml`
- **REQ-006, NFR-001** · plan archived: `M-08-e2e-smoke.md`

## 2026-06-16 — M8 Docker + quality gate

- `Dockerfile`, `docker-compose.yml`, `.github/workflows/ci.yml`, `quality.yml`
- `quality` Maven profile; `RetrievalQualityGateIntegrationTest`, `quality-report.json`
- README Docker Compose quick start
- **NFR-001, REQ-006** · plan archived: `M-09-docker-quality-gate.md`

## 2026-06-16 — M9 Prompt lab (optional)

- `promptlab` module: label normalizer, evaluator, template library, offline simulator
- `mvn verify -Pprompt-lab`; metrics merged into `quality-report.json`
- **REQ §18** · plan archived: `M-10-prompt-lab.md`

## 2026-06-16 — M10 Prompt integration (optional)

- `core/prompt/PromotedSpecialtyClassificationInstructions` — shared `react_self_reflection` winner
- `MedicalCasePrompts` — `focus=specialty` injects promoted block + case text fields
- Tests: `MedicalCasePromptsStructureTest`, extended `McpToolsContractIntegrationTest`
- **REQ §18** · plan archived: `M-11-prompt-integration.md`

## 2026-06-16 — M13 User guides (docs)

- `docs/guides/` — MCP user guide, prompt-lab guide, LM Studio manual test
- Linked from `docs/README.md`, root `README.md`, `05-deployment.md`
- Plan archived: `M-13-user-guides.md`

## 2026-06-16 — M14 Claude Desktop guide (docs)

- `docs/guides/claude-desktop-mcp.md` — config, smoke checklist, Windows MSIX notes
- Plan archived: `M-14-claude-desktop-mcp.md`

## 2026-06-16 — M12 Meta-prompting lab (M9 ext)

- `PromptLabTools` MCP (5 lab tools), `MetaPromptImprovementService`, offline stub classifier
- CI `prompt-lab` job; plan archived: `M-12-meta-prompting-lab.md`

## 2026-06-16 — M-15 Prompt-lab live chat + test gate

- `ChatPromptLabClassificationClient` (Ollama/OpenAI via `OpenAiChatModel` when `chat.enabled=true`)
- `OfflinePromptLabClassificationClient` remains default (`chat.enabled=false`)
- `PromptLabGateIntegrationTest` on `test-sample-10.csv`
- Maven `-Pprompt-lab-quality` profile for combined retrieval + prompt-lab gates
- Docs: `prompt-lab-user-guide.md`
- **Plan archived:** `M-15-prompt-lab-live-chat.md`

## 2026-06-18 — LM Studio MCP manual test (docs)

- `docs/guides/lm-studio-mcp-test-report-2026-06-18.md` — google/gemma-4-26b-a4b smoke test
- All 7 steps passed: `get_dataset_stats`, `list_specialties`, `search_cases`, `semantic_search`, `get_case`, `case-analysis` (transcription + specialty)
- Committed and pushed to `develop`

## 2026-06-18 — M-17 MCP self-description improvements

- Enhanced `spring.ai.mcp.server.instructions` with 4-step workflow narrative
- Improved tool descriptions with cross-references (e.g. "Returns case IDs that can be used with get_case")
- Improved `case-analysis` prompt description with focus options and PREDICTED_LABEL behavior
- **Plan archived:** `M-17-mcp-self-description-improvements.md`

## 2026-06-18 — M-18 MongoDB-compatible string IDs

- `IdGenerator` (ObjectId algorithm) replaces `UuidUtils` — 24-char hex, pure Java, no deps
- `V1__init_medical_cases.sql`: `id UUID` → `id TEXT`, remove `DEFAULT gen_random_uuid()`
- Domain records: `MedicalCase`, `CaseSummary`, `ClassificationEvalResult` — `UUID id` → `String id`
- Repository: `findById(String)`, `updateEmbeddingsBatch(Map<String, float[]>)`
- MCP layer: `IdGenerator.isValidId` validation, updated param descriptions
- `application.yml` instructions: 5 "UUID" mentions → "case ID"
- Docs: `medicalcase/AGENTS.md`, `retrieval/AGENTS.md`, `dataset/AGENTS.md` updated
- Tests: all 21 pass with 24-char hex IDs
- **Plan archived:** `M-18-mongodb-compatible-string-ids.md`

## Milestone status

| Milestone | Status |
|---|---|
| M1–M8 | ✅ Complete |
| M9–M10 | ✅ Complete (optional) |
| M9 ext (M-12, M-15) | ✅ Complete (optional) |
| M-17 | ✅ Complete |
| M-18 | ✅ Complete |

Canonical milestone table: [docs/01-requirements.md §14](../docs/01-requirements.md#14-milestones)
