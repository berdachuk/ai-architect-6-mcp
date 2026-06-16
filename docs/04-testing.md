# Testing Strategy — `medical-mcp-server`

**Version:** 2.0.0  
**Date:** 2026-06-16  
**Related:** [01-requirements.md](01-requirements.md) · [README.md](README.md) · [use-cases.md](use-cases.md)  
**Dataset splits:** [train CSV](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial/blob/main/medical_cases_train.csv) · [validation CSV](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial/blob/main/medical_cases_validation.csv) · [test CSV](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial/blob/main/medical_cases_test.csv)

This document proposes how to test the microservice: correctness, MCP contract compliance, performance (NFRs), and **response quality** of search/retrieval tools using the HuggingFace pre-split dataset.

---

## 1. Goals

| Goal | How |
|---|---|
| **Correctness** | Tool responses match DB state and dataset schema |
| **Contract** | MCP tool/resource/prompt shapes stable for clients |
| **Performance** | p99 latencies from requirements §8 |
| **Retrieval quality** | FTS and semantic search return relevant cases on held-out **test** split |
| **Regression** | Quality metrics tracked in CI/nightly; thresholds fail the build |

**Non-goals:** Testing LLM output of `case-analysis` prompts (non-deterministic). Test prompt *structure* and field injection only.

---

## 2. Dataset splits in testing

The [HPE tutorial dataset](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial) ships three CSV files. Use each split for a distinct testing purpose:

| Split | Rows | File | Testing role |
|---|---:|---|---|
| **train** | 1,724 | `medical_cases_train.csv` | Dev fixtures, loader IT smoke, optional quality sampling |
| **validation** | 370 | `medical_cases_validation.csv` | Tune `minSimilarity`, `topK`, FTS rank thresholds |
| **test** | 370 | `medical_cases_test.csv` | **Held-out quality benchmarks** — never used for threshold tuning |

```text
┌─────────────────────────────────────────────────────────────┐
│  train (1724)     →  unit/IT fixtures, loader tests           │
│  validation (370) →  calibrate quality gates (local/CI opt)   │
│  test (370)       →  report & gate retrieval quality        │
└─────────────────────────────────────────────────────────────┘
```

**Rule:** Quality thresholds committed to the repo must be validated on **test** only. Validation split is for experimentation before locking thresholds.

---

## 3. Test pyramid

```text
                    ┌─────────────────────┐
                    │  E2E / MCP client   │  M7 — Claude, McpSyncClient smoke
                    │  (few, manual/CI)   │
                    └──────────┬──────────┘
               ┌───────────────┴───────────────┐
               │  Quality benchmarks (test)    │  Retrieval metrics on 370 rows
               │  @Tag("quality")              │
               └───────────────┬───────────────┘
          ┌────────────────────┴────────────────────┐
          │  Integration (Testcontainers)         │  PG + pgvector, MCP tools
          │  @SpringBootTest @Tag("integration")  │
          └────────────────────┬────────────────────┘
     ┌──────────────────────────┴──────────────────────────┐
     │  Unit + Modulith                                    │  Fast, every commit
     │  ModulithArchitectureTest, service/repo units       │
     └─────────────────────────────────────────────────────┘
```

### Maven profiles (proposed)

| Profile | Tests | When |
|---|---|---|
| default | unit + modulith | Every `mvn test` |
| `integration` | + Testcontainers IT | Every PR (`mvn verify -Pintegration`) |
| `quality` | + full test-split benchmarks | Nightly or pre-release (`mvn verify -Pquality`) |
| `e2e` | + live Ollama + MCP client | Manual / staging |

---

## 4. Test layout (proposed)

```text
src/test/java/com/example/medicalmcp/
├── ModulithArchitectureTest.java
├── medicalcase/
│   └── repository/MedicalCaseRepositoryImplTest.java      # @DataJpaTest alternative: JDBC + TC
├── embedding/
│   └── service/EmbeddingServiceImplTest.java                # mock pool
├── retrieval/
│   └── service/VectorSearchServiceImplTest.java
├── dataset/
│   └── DatasetLoaderServiceImplTest.java
├── integration/
│   ├── AbstractPostgresIntegrationTest.java                 # @Testcontainers PG 17 + pgvector
│   ├── FlywaySchemaIntegrationTest.java
│   ├── DatasetLoaderIntegrationTest.java                    # load train subset
│   └── mcp/
│       ├── McpToolsContractIntegrationTest.java
│       └── McpResourcesIntegrationTest.java
└── quality/
    ├── AbstractQualityBenchmarkTest.java                  # loads test CSV ground truth
    ├── FtsRetrievalQualityTest.java
    ├── SemanticRetrievalQualityTest.java
    ├── SpecialtyFilterQualityTest.java
    └── DatasetIntegrityQualityTest.java

src/test/resources/
├── application-test.yml
├── dataset/
│   ├── train-sample-10.csv                                # fast IT
│   ├── validation-sample-20.csv                           # threshold tuning (local)
│   └── test-full.csv                                      # symlink or CI download → full 370
└── quality/
    └── expected-stats.json                                # totalCases, bySpecialty, bySplit
```

**CI download (full test split):**

```bash
curl -L -o src/test/resources/dataset/test-full.csv \
  "https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial/resolve/main/medical_cases_test.csv"
```

Same pattern for [train](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial/resolve/main/medical_cases_train.csv) and [validation](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial/resolve/main/medical_cases_validation.csv) when running full loader IT.

---

## 5. Layer-by-layer approach

### 5.1 Unit tests (fast)

| Component | What to test |
|---|---|
| `EmbeddingServiceImpl.buildEmbeddingInput` | Omits null/empty `keywords`; includes `sampleName` + `description` |
| `MedicalCaseRepositoryImpl` | SQL mapping with mocked `NamedParameterJdbcTemplate` or @JdbcTest |
| `VectorSearchServiceImpl` | Cosine query params, specialty pre-filter |
| `DatasetLoaderServiceImpl` | Split assignment from filename; idempotent skip |
| Domain records | JSON serialization of `CaseSummary`, `SemanticMatch` |

### 5.2 Modulith boundary test

```java
@ApplicationModuleTest
class ModulithArchitectureTest {
    @Test
    void verifyModuleBoundaries() {
        ApplicationModules.of(MedicalMcpApplication.class).verify();
    }
}
```

Runs on every `mvn test` — blocks illegal cross-module dependencies.

### 5.3 Integration tests (Testcontainers)

**Infrastructure:** `pgvector/pgvector:pg17` container, Flyway `V1`, load `train-sample-10.csv`.

| Test class | Verifies |
|---|---|
| `FlywaySchemaIntegrationTest` | Extensions, indexes, `VECTOR(768)`, generated `fts` |
| `DatasetLoaderIntegrationTest` | Row count, `split` column, UUID assigned |
| `McpToolsContractIntegrationTest` | Each tool callable; response JSON schema |
| `McpResourcesIntegrationTest` | `medical://cases/{id}`, `medical://stats` |

**Embedding in IT:**

| Option | Pros | Cons |
|---|---|---|
| **WireMock** OpenAI-compatible `/v1/embeddings` | Deterministic, fast CI | Vectors not semantically meaningful |
| **Testcontainers Ollama** (optional profile) | Real `nomic-embed-text:v1.5` | Slow, GPU/host dependency |
| **Precomputed embeddings** in test SQL | Fast semantic IT | Fixture maintenance |

**Recommendation:** Default CI uses WireMock fixed vectors for contract IT; `quality` profile uses real Ollama or precomputed embeddings from a one-time offline job.

### 5.4 MCP contract tests

Invoke tools via Spring test context (direct bean call) or `McpSyncClient` against `@SpringBootTest(webEnvironment = RANDOM_PORT)`.

| Tool | Contract assertions |
|---|---|
| `search_cases` | Array of objects with `id`, `sampleName`, `description`, `medicalSpecialty`, `keywords`, `split`; no `transcription` |
| `get_case` | All HF fields + `id`, `split`, `createdAt`; `transcription` non-null |
| `semantic_search` | `caseSummary` + `similarity` in [0,1]; ordered by similarity desc |
| `list_specialties` | Exactly 13 entries; labels match requirements §2 table |
| `get_dataset_stats` | `totalCases`, `bySpecialty`, `bySplit` keys |

**Negative cases:**

- Invalid UUID → structured error / empty optional
- Unknown `specialty` filter → empty list (not 500)
- `limit` > 50 → clamped to 50
- Invalid `split` → 400 or empty (define behavior; test it)

---

## 6. Response quality testing

Quality tests measure whether MCP tool responses are **useful and faithful** to the dataset—not whether an LLM summarizes well.

### 6.1 Ground-truth model

Each row in [medical_cases_test.csv](https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial/blob/main/medical_cases_test.csv) provides:

| Field | Use in quality tests |
|---|---|
| `description` | Query text for semantic search; FTS term source |
| `transcription` | FTS ground truth (terms appear in note) |
| `sample_name` | FTS exact/prefix queries |
| `medical_specialty` | **Relevance label** — top-K specialty match rate |
| `keywords` | FTS query terms when non-null |

Assign stable **test IDs** during fixture load (deterministic UUID v5 from `split + row_index` or content hash) so benchmarks are reproducible.

### 6.2 Dataset integrity benchmarks

**Class:** `DatasetIntegrityQualityTest`  
**Split:** all loaded data (or test only)

| Metric | Method | Threshold (initial) |
|---|---|---|
| Row count | `get_dataset_stats().totalCases()` | 370 (test fixture) or 2464 (full) |
| Split counts | `bySplit` | train=1724, validation=370, test=370 |
| Specialty count | `list_specialties().size()` | 13 |
| Round-trip fidelity | Load CSV row → `get_case(id)` | All 5 HF fields byte-equal |
| Cache | Two `get_dataset_stats` within 60s | Same object / same counts |

### 6.3 FTS quality (`search_cases`)

**Class:** `FtsRetrievalQualityTest`  
**Split:** **test** (370 rows)

For each test row (or stratified sample of N=100):

1. Build query `q` from row:
   - **Q1:** random 2–3 word phrase from `transcription`
   - **Q2:** full `sample_name`
   - **Q3:** first comma-separated token from `keywords` (skip if null)
2. Call `search_cases(query=q, limit=10)`
3. Score:

| Metric | Definition |
|---|---|
| **Hit@K** | Fraction where source row `id` appears in top-K results |
| **Specialty@K** | Fraction where ≥1 result in top-K has same `medical_specialty` |
| **MRR** | Mean reciprocal rank of source row |

**Initial gates (test split, K=10):**

| Query type | Hit@10 | Specialty@10 |
|---|---|---|
| Q2 (`sample_name`) | ≥ 0.95 | ≥ 0.95 |
| Q1 (transcription phrase) | ≥ 0.70 | ≥ 0.60 |
| Q3 (keyword token) | ≥ 0.80 | ≥ 0.70 |

Tune on **validation**; lock thresholds from **test** evaluation.

### 6.4 Semantic quality (`semantic_search`)

**Class:** `SemanticRetrievalQualityTest`  
**Split:** **test**  
**Requires:** real or precomputed `nomic-embed-text:v1.5` embeddings

For each test row:

1. `query = description` (or `sample_name + description`)
2. `semantic_search(query, topK=5, minSimilarity=0.0)` — no threshold during measurement
3. Score:

| Metric | Definition |
|---|---|
| **Self@1** | Source row is rank 1 |
| **Self@5** | Source row in top 5 |
| **Specialty@5** | ≥1 top-5 result shares `medical_specialty` |
| **Mean similarity@1** | Average cosine at rank 1 |

**Initial gates (test split):**

| Metric | Threshold |
|---|---|
| Self@1 | ≥ 0.35 |
| Self@5 | ≥ 0.65 |
| Specialty@5 | ≥ 0.85 |
| Mean similarity@1 | ≥ 0.75 |

**Cross-specialty negative check:** Query row from Orthopedic; results should not be dominated by unrelated specialties (optional: max 2 of top-5 from wrong specialty).

### 6.5 Specialty filter quality

**Class:** `SpecialtyFilterQualityTest`

For each of 13 labels:

1. `search_cases(query="patient", specialty=label, limit=50)`
2. Assert **100%** of results have `medicalSpecialty == label` (exact string)
3. `semantic_search(query="clinical case", specialty=label, topK=10)` — same assertion

### 6.6 Split filter quality

For `split` ∈ {`train`, `validation`, `test`}:

1. `search_cases(query="the", split=split, limit=50)` — high-recall probe
2. Assert all results have matching `split`
3. Count ≈ min(50, split size) when query is broad

### 6.7 Performance quality (NFR)

**Class:** `McpLatencyIntegrationTest`  
**Data:** loaded test subset, warm JVM

| Tool | p99 target (requirements) | Test approach |
|---|---|---|
| `search_cases` | < 100 ms | JUnit + Micrometer or 100 iterations, assert p99 |
| `get_case` | < 30 ms | by known UUID |
| `get_dataset_stats` | < 20 ms | cached |
| `semantic_search` | < 400 ms excl. embed | mock embed latency separately; measure DB portion |

Report embed latency separately — depends on Ollama, not the microservice DB path.

---

## 7. Quality report format

Quality profile emits a machine-readable report for trend tracking:

```text
target/test-output/quality-report.json
```

```json
{
  "dataset": "hpe-ai/medical-cases-classification-tutorial",
  "split": "test",
  "rows": 370,
  "embeddingModel": "nomic-embed-text:v1.5",
  "timestamp": "2026-06-16T12:00:00Z",
  "fts": {
    "sampleName_hit10": 0.97,
    "transcriptionPhrase_hit10": 0.74,
    "keyword_hit10": 0.82,
    "mrr": 0.61
  },
  "semantic": {
    "self_at_1": 0.41,
    "self_at_5": 0.71,
    "specialty_at_5": 0.89,
    "mean_similarity_at_1": 0.78
  },
  "integrity": {
    "specialty_count": 13,
    "round_trip_pass": true
  },
  "passed": true
}
```

Store reports as CI artifacts; fail build when `passed: false`.

**Future:** Optional [prompt-lab profile](future/prompt-lab.md) ([requirements §18](01-requirements.md#18-future-scope-optional), milestones M9/M10)

---

## 8. Example quality test (sketch)

```java
@Tag("quality")
@SpringBootTest
@Testcontainers
class SemanticRetrievalQualityTest extends AbstractQualityBenchmarkTest {

    @Autowired VectorSearchService vectorSearch;
    @Autowired EmbeddingService embeddingService;

  @ParameterizedTest
  @CsvFileSource(resources = "/dataset/test-full.csv", numLinesToSkip = 1)
  void selfRetrievalInTop5(String description, String transcription,
      String sampleName, String specialty, String keywords) {
    UUID id = fixtureIdFor(sampleName, description);
    List<SemanticMatch> hits = vectorSearch.semanticSearch(
        embeddingService.embedAsFloatArray(description), null, 5, 0.0);
    boolean inTop5 = hits.stream()
        .anyMatch(m -> m.caseSummary().id().equals(id));
    qualityRecorder.record("self_at_5", inTop5);
  }

  @AfterAll
  void assertGates() {
    assertThat(qualityRecorder.rate("self_at_5")).isGreaterThanOrEqualTo(0.65);
  }
}
```

---

## 9. CI pipeline (proposed)

```yaml
# .github/workflows/test.yml (conceptual)
jobs:
  unit:
    runs-on: ubuntu-latest
    steps:
      - run: mvn -B test

  integration:
    runs-on: ubuntu-latest
    steps:
      - run: mvn -B verify -Pintegration

  quality:
    runs-on: ubuntu-latest
    if: github.event_name == 'schedule' || contains(github.ref, 'release')
    services:
      ollama: ...   # or use precomputed embeddings
    steps:
      - run: curl -L -o src/test/resources/dataset/test-full.csv \
          https://huggingface.co/datasets/hpe-ai/medical-cases-classification-tutorial/resolve/main/medical_cases_test.csv
      - run: ollama pull nomic-embed-text:v1.5
      - run: mvn -B verify -Pquality
      - uses: actions/upload-artifact@v4
        with:
          name: quality-report
          path: target/quality-report.json
```

---

## 10. Mapping to milestones

| Milestone | Tests delivered |
|---|---|
| M1 | `ModulithArchitectureTest`, `FlywaySchemaIntegrationTest` |
| M2 | `DatasetLoaderIntegrationTest` (train CSV sample) |
| M3 | Repository IT, `FtsRetrievalQualityTest` (subset) |
| M4 | Embedding IT, `SemanticRetrievalQualityTest` |
| M5 | `McpToolsContractIntegrationTest`, `McpResourcesIntegrationTest` |
| M6 | Config property binding tests, cache TTL test |
| M7 | `McpSyncClient` smoke, Claude Desktop checklist |
| M8 | Docker health + full **test** split quality gate in nightly |

**Optional:** M9 prompt-lab, M10 prompt integration — [requirements §18](01-requirements.md#18-future-scope-optional), [future/prompt-lab.md](future/prompt-lab.md).

---

## 11. Manual smoke checklist (M7)

- [ ] `get_dataset_stats` → 2464 rows, 13 specialties
- [ ] `list_specialties` → includes `Radiology` (50)
- [ ] `search_cases(query="pacemaker", specialty="Cardiovascular / Pulmonary")` → non-empty
- [ ] `semantic_search(query="knee MRI tear", specialty="Orthopedic")` → relevant titles
- [ ] `get_case` round-trip matches CSV for one known UUID
- [ ] `medical://stats` resource matches tool output
- [ ] `case-analysis(caseId, focus=transcription)` returns populated template
- [ ] p99 latency spot-check under requirements targets

---

## 12. What not to test

| Item | Reason |
|---|---|
| LLM narrative quality of `case-analysis` | Non-deterministic; out of MCP server scope |
| HuggingFace download in every unit test | Network flake; use fixtures |
| Full 2464-row embed on every commit | Too slow; nightly quality profile |
| Classifier accuracy vs HPE tutorial model | Server is retrieval, not training |
| `sample_name` uniqueness | Documented non-property; use UUID |

---

## Related documentation

- [01-requirements.md §8 — NFRs](01-requirements.md#8-non-functional-requirements)
- [use-cases.md — W03 test split workflow](use-cases.md#w03--developer-evaluate-classifier-on-test-split)
- [01-requirements.md §14 — Milestones](01-requirements.md#14-milestones)
