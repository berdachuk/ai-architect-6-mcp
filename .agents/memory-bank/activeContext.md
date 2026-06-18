# Active context

**Updated:** 2026-06-18

## Current focus

**M-17 complete** — MCP self-description improvements: enhanced `instructions` with 4-step workflow, improved tool/prompt descriptions with cross-references.

**Active plan:** **M-16** — CI combined quality gates ([plan](../plans/M-16-ci-combined-quality-gates.md)).

Core M1–M8 and optional M9–M15 complete. User guides complete. M-17 complete.

## Verified

- `mvn verify -Pprompt-lab` — offline stub + gate IT on test split
- `chat.enabled=false` default; live chat wired when `chat.enabled=true`
- Default MCP surface: 5 tools, 2 resources, 1 prompt (no prompt-lab profile)
- M-17: `instructions` field provides full workflow narrative; tool descriptions reference downstream tools
