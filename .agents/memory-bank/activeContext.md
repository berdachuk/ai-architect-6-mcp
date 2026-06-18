# Active context

**Updated:** 2026-06-16

## Current focus

**M-15 complete** — live chat client (`ChatPromptLabClassificationClient`), offline stub default, test-split gate IT, `-Pprompt-lab-quality` Maven profile.

**Active plan:** **M-16** — CI combined quality gates ([plan](../plans/M-16-ci-combined-quality-gates.md)).

Core M1–M8 and optional M9–M15 complete. User guides complete.

## Verified

- `mvn verify -Pprompt-lab` — offline stub + gate IT on test split
- `chat.enabled=false` default; live chat wired when `chat.enabled=true`
- Default MCP surface: 5 tools, 2 resources, 1 prompt (no prompt-lab profile)
