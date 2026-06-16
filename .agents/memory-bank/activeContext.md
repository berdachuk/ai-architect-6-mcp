# Active context

**Updated:** 2026-06-16

## Current focus

**M-12 complete** — prompt-lab MCP tools (`evaluate_specialty_prompt`, `compare_specialty_prompts`, `improve_specialty_prompt`, `gate_specialty_prompt`, `list_prompt_templates`) under `@Profile("prompt-lab")` only.

**Active plan:** **M-15** — live chat client + test-split gate ([plan](../plans/M-15-prompt-lab-live-chat.md)).

Core M1–M8 and optional M9–M10 complete. User guides complete.

## Verified

- `mvn verify -Pprompt-lab` — offline stub + MCP tools IT
- Default MCP surface: 5 tools, 2 resources, 1 prompt (no prompt-lab profile)
