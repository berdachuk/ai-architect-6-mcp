# Use Cases — `medical-mcp-server`

**Version:** 1.6.0  
**Date:** 2026-06-16  
**Requirements:** [PRD.md](PRD.md) · [PLAN.md](PLAN.md)  
**Dataset:** [hpe-ai/medical-cases-classification-tutorial](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial)

This document lists **all supported use cases** for the MCP server. Each maps to dataset fields or server-derived indexes (FTS, embeddings, UUID, split). Clinical inference beyond stored text is out of scope.

---

## Actors

| Actor | Description |
|---|---|
| **Claude Desktop user** | Human using Claude with MCP over SSE (`:8092/sse`) |
| **med-expert-match-ce agent** | Spring AI `McpSyncClient` consuming tools during GraphRAG / expert matching |
| **LLM orchestrator** | Any MCP-capable agent (Cursor, custom bot) chaining tools |
| **Developer / evaluator** | Engineer testing retrieval, splits, or embedding alignment |
| **Operator** | Person deploying, loading data, monitoring health |

---

## MCP capability matrix

| Capability | Endpoints | Use case IDs |
|---|---|---|
| Tools | 5 | UC-T01 … UC-T35 |
| Resources | 2 | UC-R01 … UC-R04 |
| Prompts | 1 | UC-P01 … UC-P06 |
| Completions | — | *Not supported* |

---

## Tool use cases

### `search_cases` — full-text search (tsvector)

| ID | Title | Actor | Scenario |
|---|---|---|---|
| UC-T01 | Keyword lookup in transcriptions | Claude user | User asks *"Find cases mentioning pacemaker interrogation"* → agent calls `search_cases(query="pacemaker interrogation")` |
| UC-T02 | Search by procedure name | LLM orchestrator | `search_cases(query="laparoscopic pyeloplasty")` matches `sample_name`, `description`, or `transcription` |
| UC-T03 | Filter by specialty | med-expert-match-ce | `search_cases(query="chest pain", specialty="Cardiovascular / Pulmonary")` — exact label required |
| UC-T04 | Filter by train split | Developer | `search_cases(query="MRI", split="train")` for ML training-set exploration |
| UC-T05 | Filter by validation split | Developer | `search_cases(query="consultation", split="validation")` |
| UC-T06 | Filter by test split | Developer | `search_cases(query="biopsy", split="test")` for held-out evaluation |
| UC-T07 | Specialty + split combined | Evaluator | `search_cases(query="knee", specialty="Orthopedic", split="test", limit=20)` |
| UC-T08 | Broad specialty scan | Claude user | `search_cases(query="diabetes", specialty="Nephrology")` |
| UC-T09 | Limit result size | Any | `search_cases(query="surgery", limit=5)` — default 10, max 50 |
| UC-T10 | Lab / finding terms in long notes | Claude user | FTS over `transcription` (not in semantic index) — e.g. `query="BNP elevated"` |
| UC-T11 | Keyword field search | LLM orchestrator | `search_cases(query="colonoscopy")` hits rows where term appears in nullable `keywords` |
| UC-T12 | Empty keywords row | Developer | Row with null `keywords` still findable via `description` / `transcription` FTS |
| UC-T13 | Discover case UUID | Any | User needs `id` for `get_case` → search first, pick UUID from `CaseSummary` |

**Example flow (UC-T01 + UC-T13):**

```
1. search_cases(query="pacemaker ICD", limit=10)
2. get_case(id="<uuid-from-step-1>")
```

---

### `get_case` — full record by UUID

| ID | Title | Actor | Scenario |
|---|---|---|---|
| UC-T14 | Read full transcription | Claude user | After search, fetch complete clinical note for summarization |
| UC-T15 | Verify specialty label | Evaluator | Compare `medical_specialty` with model prediction for a known UUID |
| UC-T16 | Inspect keywords | Developer | Check whether `keywords` is null or populated for a case |
| UC-T17 | Audit split assignment | Developer | Confirm `split` is `train`, `validation`, or `test` for a row |
| UC-T18 | Compare description vs transcription | LLM orchestrator | Short `description` vs long `transcription` for same `id` |
| UC-T19 | Case handoff to prompt | Claude user | Obtain UUID → pass to `case-analysis` prompt |

**Precondition:** Valid server-assigned UUID (not `sample_name`).

---

### `semantic_search` — vector similarity (pgvector)

| ID | Title | Actor | Scenario |
|---|---|---|---|
| UC-T20 | Find clinically similar cases | med-expert-match-ce | `semantic_search(query="patient with atrial fibrillation and slow ventricular response")` |
| UC-T21 | Similar cases in one specialty | Claude user | `semantic_search(query="acute kidney failure", specialty="Nephrology", topK=5)` |
| UC-T22 | High-confidence matches only | Evaluator | `semantic_search(query="...", minSimilarity=0.85)` |
| UC-T23 | Wider recall | LLM orchestrator | `semantic_search(query="...", minSimilarity=0.60, topK=10)` |
| UC-T24 | Cross-specialty semantic browse | Claude user | No `specialty` filter — compare cases across all 13 labels |
| UC-T25 | Orthopedic similarity | Developer | `semantic_search(query="ACL reconstruction failure", specialty="Orthopedic")` |
| UC-T26 | Radiology similarity | Developer | `semantic_search(query="pulmonary embolism CT angiography", specialty="Radiology")` |
| UC-T27 | Progress feedback | med-expert-match-ce | Long-running embed + search reports 0 % → 50 % → 100 % via MCP progress |
| UC-T28 | GraphRAG alignment | med-expert-match-ce | Same `nomic-embed-text:v1.5` @ 768 dims as GraphRAG — vectors interoperable |
| UC-T29 | Semantic then full text | LLM orchestrator | `semantic_search` → `get_case` on top match UUID |

**Note:** Semantic index uses `{sampleName}. {description} {keywords}` — not `transcription`. Use UC-T01/UC-T10 for transcription-heavy queries.

---

### `list_specialties` — taxonomy discovery

| ID | Title | Actor | Scenario |
|---|---|---|---|
| UC-T30 | Discover valid specialty labels | Claude user | Agent calls `list_specialties()` before applying `specialty` filter |
| UC-T31 | Dataset balance overview | Evaluator | See counts: Cardiovascular / Pulmonary (742) … Radiology (50) |
| UC-T32 | UI / agent label picker | LLM orchestrator | Present 13 exact strings to user — avoids invalid filter values |
| UC-T33 | Confirm 13-class problem | Developer | Validate loaded data matches HuggingFace classification task |

---

### `get_dataset_stats` — aggregate metrics

| ID | Title | Actor | Scenario |
|---|---|---|---|
| UC-T34 | Dataset size check | Operator | `totalCases: 2464`, splits `1724 / 370 / 370` |
| UC-T35 | Specialty distribution chart | Evaluator | `bySpecialty` map for imbalance analysis |
| UC-T36 | Split distribution | Developer | `bySplit` for train/validation/test ratios |
| UC-T37 | Cached stats read | Any | Repeated calls within 60 s served from Caffeine cache |

---

## Resource use cases

| ID | Title | Actor | Scenario |
|---|---|---|---|
| UC-R01 | Attach case as MCP resource | Claude Desktop | Client reads `medical://cases/{uuid}` — full JSON including `transcription` |
| UC-R02 | Stats resource snapshot | LLM orchestrator | `medical://stats` — same payload as `get_dataset_stats` |
| UC-R03 | Resource-first workflow | med-expert-match-ce | Agent discovers resource URI from tool result, fetches without second tool call |
| UC-R04 | Pin case for session | Claude user | User references `medical://cases/{id}` across multi-turn chat |

---

## Prompt use cases — `case-analysis`

| ID | Title | Actor | Args | Scenario |
|---|---|---|---|---|
| UC-P01 | Full case review | Claude user | `caseId`, `focus=all` | Inject description + transcription + keywords + specialty |
| UC-P02 | Summary-only analysis | Claude user | `caseId`, `focus=description` | Emphasize short visit summary |
| UC-P03 | Deep note review | med-expert-match-ce | `caseId`, `focus=transcription` | Full procedure / HPI text for expert matching context |
| UC-P04 | Keyword-focused review | Evaluator | `caseId`, `focus=keywords` | Template omits section when `keywords` is null |
| UC-P05 | Specialty classification study | Developer | `caseId`, `focus=specialty` | Discuss `medical_specialty` label vs case content |
| UC-P06 | Search → analyze pipeline | Any | UUID from UC-T13 | `search_cases` / `semantic_search` → `case-analysis` |

**Precondition:** `caseId` is server UUID from a prior tool call (no autocomplete — completions disabled).

---

## End-to-end workflows

### W01 — Claude Desktop: “Find and summarize a cardiology case”

```
list_specialties()                                    # optional: confirm label
search_cases(query="cardiac catheterization",
             specialty="Cardiovascular / Pulmonary", limit=3)
get_case(id="<uuid>")
case-analysis(caseId="<uuid>", focus="transcription")
```

### W02 — med-expert-match-ce: GraphRAG supplemental retrieval

```
semantic_search(query="<user clinical question>", topK=5, minSimilarity=0.70)
get_case(id="<best-match-uuid>")                     # or medical://cases/{id}
# Merge with local GraphRAG context (same embedding model)
```

### W03 — Developer: evaluate classifier on test split

```
get_dataset_stats()                                   # confirm test=370
search_cases(query="", specialty="Neurology", split="test", limit=50)
# Compare medical_specialty to external model output
```

### W04 — Keyword vs semantic retrieval comparison

```
search_cases(query="colon polyp", limit=10)           # FTS on transcription
semantic_search(query="colon polyp screening", topK=10)
# Compare overlap of UUIDs
```

### W05 — Specialty-scoped semantic + FTS hybrid

```
semantic_search(query="knee effusion MRI", specialty="Orthopedic", topK=5)
search_cases(query="effusion", specialty="Orthopedic", split="train", limit=10)
```

### W06 — Operator: first-time startup validation

```
# After server load (CSV → DB → embeddings)
get_dataset_stats()          → totalCases=2464
list_specialties()           → 13 entries
search_cases(query="test", limit=1)
semantic_search(query="chest pain", topK=1)
# Actuator: GET /actuator/health
```

### W07 — Explore underrepresented specialty

```
list_specialties()           → Radiology: 50
search_cases(query="CT", specialty="Radiology", limit=20)
semantic_search(query="pulmonary embolism protocol", specialty="Radiology")
```

### W08 — Validation-set spot check

```
search_cases(query="psych consult", split="validation", limit=5)
get_case(id="<uuid>")
case-analysis(caseId="<uuid>", focus="all")
```

---

## Integration scenarios

| ID | Integration | Use |
|---|---|---|
| UC-I01 | **Claude Desktop** | SSE `http://localhost:8092/sse` — all tool/resource/prompt use cases |
| UC-I02 | **med-expert-match-ce** | `spring.ai.mcp.client.sse.connections.medical-dataset` — semantic + case fetch during expert workflows |
| UC-I03 | **Shared embedding space** | Vectors from this server compatible with med-expert-match-ce GraphRAG (same model/dims) |
| UC-I04 | **Cursor / IDE agent** | MCP tools for codebase-adjacent medical dataset exploration |
| UC-I05 | **Custom MCP client** | Any SYNC SSE client implementing MCP 2024-11-05 |

---

## Operational use cases

| ID | Title | Scenario |
|---|---|---|
| UC-O01 | Initial dataset load | Startup loader ingests 3 CSV files; pass 1 FTS-ready, pass 2 embeddings |
| UC-O02 | Idempotent restart | `COUNT(*) > 0` → skip reload |
| UC-O03 | Disable loader post-load | `MEDICALMCP_DATASET_LOADER_ENABLED=false` |
| UC-O04 | Multi-endpoint embedding | Scale Ollama via `EmbeddingEndpointPool` additional endpoints |
| UC-O05 | Health monitoring | `/actuator/health` — DB + embedding pool readiness |
| UC-O06 | Docker Compose deploy | `postgres` + `medical-mcp-server`; Ollama on host |
| UC-O07 | Modulith CI gate | `mvn verify` → `ApplicationModules.verify()` |

---

## Use cases by medical specialty (all 13)

Each specialty supports the same tool pattern with `specialty="<exact label>"`:

| Specialty | Example semantic query | Example FTS query |
|---|---|---|
| Cardiovascular / Pulmonary | `heart failure consult` | `pacemaker` |
| Orthopedic | `knee MRI partial tear` | `bunionectomy` |
| Neurology | `diplopia double vision` | `lumbar puncture` |
| Gastroenterology | `colonoscopy bleeding` | `sigmoidoscopy` |
| Obstetrics / Gynecology | `bacterial vaginosis` | `tubal ligation` |
| Hematology - Oncology | `lung cancer hospice` | `mesothelioma` |
| Neurosurgery | `subdural hematoma craniotomy` | `frontotemporal` |
| ENT - Otolaryngology | `otitis media drainage` | `ear pain` |
| Nephrology | `kidney transplant evaluation` | `dialysis` |
| Psychiatry / Psychology | `involuntary psychiatric hold` | `assaultive behavior` |
| Ophthalmology | `eye examination` | `ophthalmology` |
| Pediatrics - Neonatal | `chronic ear infections toddler` | `otitis media` |
| Radiology | `CT pulmonary embolism` | `MRI spine` |

---

## Explicitly out of scope

These are **not** supported use cases (see PRD non-goals):

| Request | Why not |
|---|---|
| Diagnose a real patient | Tutorial dataset only; no clinical inference API |
| Train / fine-tune a classifier via MCP | No training endpoints |
| HuggingFace live sync | One-time load at startup |
| REST API access | MCP-only external contract |
| Search by `sample_name` alone as ID | Not unique — use UUID |
| MCP argument completion for `caseId` | Removed — incompatible with UUID model |
| `focus=diagnosis` or `focus=treatment` on prompt | Not dataset columns |
| Re-embed with different model without DB reset | `EMBEDDING_DIMENSIONS` fixed at 768 |
| Mix vector dimensions in one DB | Schema enforces `VECTOR(768)` |

---

## Quick reference — choose the right tool

| User intent | Tool |
|---|---|
| Exact words / terms in clinical notes | `search_cases` |
| “Cases like this description” (meaning) | `semantic_search` |
| Full note text | `get_case` or `medical://cases/{id}` |
| What specialties exist? | `list_specialties` |
| How big is the dataset? | `get_dataset_stats` or `medical://stats` |
| LLM prompt with case context | `case-analysis` |
| ML train vs test exploration | `search_cases` or `semantic_search` + `split` param |

---

## Related documentation

- [PRD.md §6 — MCP Surface](PRD.md#6-mcp-surface)
- [PRD.md §2 — Dataset schema](PRD.md#2-source-dataset)
- [PRD.md §14](PRD.md#14-milestones) — milestones with test deliverables
- [PRD.md §17](PRD.md#17-testing--quality-assurance) — testing summary
