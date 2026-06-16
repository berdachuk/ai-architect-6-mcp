# Prompt lab user guide

**Version:** 1.0  
**Profile:** `prompt-lab` (optional — **off by default**)

This guide explains how to run the **offline prompt evaluation lab**: compare specialty-classification templates, read metrics, and understand how winners are promoted into the production `case-analysis` prompt.

Design background: [future/prompt-lab.md](../future/prompt-lab.md). Production MCP surface is unchanged when this profile is disabled.

---

## What prompt lab is

The `promptlab` module evaluates **prompt templates** for predicting `medical_specialty` from case text. It is:

- An **evaluation and tuning** workflow for developers and prompt engineers
- **Not** a patient-facing classification API
- **Not** adding tools to the default MCP server (5 tools, 2 resources, 1 prompt)

Live LLM evaluation and MCP lab tools are available under the `prompt-lab` profile. Offline Maven tests run without a chat model by default.

---

## When to use it

- Compare ReAct vs naive prompt structures before changing production prompts
- Gate template quality on the **validation** split (tune here, lock on **test**)
- Verify promotion candidates match `PromotedSpecialtyClassificationInstructions`

---

## Enable prompt-lab

Activate the Spring profile and enable properties:

```yaml
spring:
  profiles:
    active: prompt-lab

medicalmcp:
  prompt-lab:
    enabled: true
    eval-split: validation
    min-accuracy: 0.55
```

Reference fixture config: `src/test/resources/application-prompt-lab.yml`.

Beans are gated by `@Profile("prompt-lab")` on `PromptLabConfig`. With the profile off, no prompt-lab code loads and the default MCP surface is identical to production.

---

## Template library

| Template id | Description | Typical role |
|---|---|---|
| `bad` | Naive “classify the specialty” prompt | Baseline (~30 % on small samples in reference report) |
| `react` | ReAct: Thought → Action → Observation → Answer | Strong intermediate |
| `react_self_reflection` | ReAct + reflection before answer | **Winner** — promoted to `case-analysis` `focus=specialty` |

Templates live in `PromptTemplateLibrary`. The production copy of the winner is `core/prompt/PromotedSpecialtyClassificationInstructions`.

---

## MCP lab tools (`prompt-lab` profile only)

When the server runs with `spring.profiles.active=prompt-lab`, five additional MCP tools are registered:

| Tool | Purpose |
|---|---|
| `evaluate_specialty_prompt` | Run a template on N cases; optional saved eval run id |
| `compare_specialty_prompts` | Rank multiple templates by accuracy |
| `improve_specialty_prompt` | Meta-improve using failure examples from prior eval |
| `gate_specialty_prompt` | Gate template on test (or chosen) split |
| `list_prompt_templates` | Built-in + meta-improved templates |

Default profile (**no** `prompt-lab`) keeps the production surface: 5 tools, 2 resources, 1 prompt.

### Classification client

CI and default `prompt-lab` runs use an **offline stub classifier** (`OfflinePromptLabClassificationClient`). Live Ollama/OpenAI chat wiring is optional future work — enable `medicalmcp.prompt-lab.chat.enabled` when implemented.

---

## Run offline evaluation

From the project root (use **WSL** on Windows for Docker/Testcontainers):

```bash
mvn verify -Pprompt-lab
```

### What runs

`PromptLabOfflineEvalIntegrationTest` and `PromptLabToolsIntegrationTest` (tag `prompt-lab`):

1. Loads `validation-sample-10.csv` fixture into Postgres
2. Evaluates `react_self_reflection` with an accurate offline simulator → **expects gate pass**
3. Evaluates `bad` with a poor simulator → **expects gate fail**

This proves the harness and gate wiring without calling a live chat model.

### Gate configuration

| Property | Default | Meaning |
|---|---|---|
| `medicalmcp.prompt-lab.eval-split` | `validation` | Split used for eval rows |
| `medicalmcp.prompt-lab.min-accuracy` | `0.55` | Minimum accuracy to pass gate |

---

## Read results

After `mvn verify -Pprompt-lab`, open:

```
target/test-output/quality-report.json
```

Look for the `promptLab` section (written by `PromptLabQualityReporter`):

```json
{
  "promptLab": {
    "templateId": "react_self_reflection",
    "split": "validation",
    "total": 10,
    "correct": 10,
    "accuracy": 1.0,
    "minAccuracy": 0.55,
    "passed": true,
    "perSpecialtyErrors": { }
  },
  "passed": true
}
```

| Field | Meaning |
|---|---|
| `templateId` | Template under test |
| `accuracy` | Correct / total |
| `minAccuracy` | Configured gate threshold |
| `passed` | Whether this template meets the gate |
| `perSpecialtyErrors` | Misclassification counts by specialty |

If a retrieval quality report already exists, prompt-lab metrics are **merged** into the same file.

---

## Promotion workflow (M10)

Promotion from lab to production is **manual**:

1. Run offline (or future live) eval on **validation** split.
2. Compare accuracy in `quality-report.json` → `promptLab`.
3. Gate on **test** split before changing production (discipline per [04-testing.md](../04-testing.md)).
4. Update `PromotedSpecialtyClassificationInstructions` and `MedicalCasePrompts` (`focus=specialty`) — already done for `react_self_reflection`.

See also: `src/main/java/com/example/medicalmcp/promptlab/PROMOTION.md`.

There is **no** auto-deploy from JSON metrics to production prompts.

---

## Label normalization

Eval compares model output to dataset `medical_specialty` using:

- **`SpecialtyLabelNormalizer`** — maps aliases (e.g. `cardiology` → `Cardiovascular / Pulmonary`) to 13 canonical HF labels
- **`PREDICTED_LABEL: <snake_case>`** — parse contract in model output

Example canonical labels in snake_case: `cardiovascular_pulmonary`, `obstetrics_gynecology`, `ent_otolaryngology`.

Unit tests: `SpecialtyLabelNormalizerTest`, `PredictedLabelExtractorTest`.

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| Prompt-lab beans not found | Set `spring.profiles.active=prompt-lab` and `medicalmcp.prompt-lab.enabled=true` |
| `bad` template fails gate | Expected in offline IT — demonstrates gate rejects poor templates |
| `mvn verify -Pprompt-lab` fails on Windows | Run Maven from WSL ([techContext](../../.agents/memory-bank/techContext.md)) |
| No `promptLab` in report | Ensure IT ran `PromptLabQualityReporter.mergePromptLabMetrics` (passing test path) |

---

## Related documentation

- [mcp-user-guide.md](mcp-user-guide.md) — production `case-analysis` and `focus=specialty`
- [future/prompt-lab.md](../future/prompt-lab.md) — full proposal and phases
- [01-requirements.md §18](../01-requirements.md#18-future-scope-optional) — optional scope
