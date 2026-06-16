# Prompt template promotion (M9 → M11)

Winning templates from `mvn verify -Pprompt-lab` are promoted manually into production MCP prompts.

## Flow

1. Run offline eval on validation split (`PromptLabOfflineEvalIntegrationTest` or future `evaluate_specialty_prompt` MCP tool).
2. Compare template accuracy in `target/test-output/quality-report.json` → `promptLab` section.
3. Gate on test split before promotion ([M-11 plan](../../.agents/plans/M-11-prompt-integration.md)).
4. Copy improved system instructions into `MedicalCasePrompts.buildAnalysisMessage()` or a dedicated `focus=specialty` block — **do not** add new default-profile MCP tools.

## Constraints

- Default profile: no prompt-lab beans (`@Profile("prompt-lab")` on `PromptLabConfig` only).
- Production `case-analysis` remains dataset-field-only; classification eval stays in `promptlab` module.
