# Manual MCP testing with Claude Desktop

**Version:** 1.0  
**Client:** [Claude Desktop](https://claude.ai/download)  
**MCP server:** `http://localhost:8092/sse`

Step-by-step guide to connect **Claude Desktop** to **medical-mcp-server** and manually exercise all tools, resources, and the `case-analysis` prompt.

---

## Overview

| Component | Role |
|---|---|
| **Claude Desktop** | MCP host — Anthropic-hosted model + tool calling |
| **medical-mcp-server** | Remote SSE MCP server — dataset tools on port 8092 |
| **PostgreSQL + pgvector** | Case storage and vector index |
| **Ollama** | Embedding API for `semantic_search` only (`nomic-embed-text:v1.5`) |

Claude Desktop does **not** run embeddings. The MCP server calls Ollama when you use `semantic_search`. See [01-requirements.md §3](../01-requirements.md#3-embedding-model).

### Architecture

```text
┌──────────────────┐     MCP/SSE      ┌──────────────────────────┐
│ Claude Desktop   │ ───────────────► │  medical-mcp-server:8092 │
│ (hosted model)   │                  └────────────┬─────────────┘
└──────────────────┘                               │
                                     ┌─────────────┴─────────────┐
                                     ▼                           ▼
                             PostgreSQL + pgvector            Ollama :11434
                                                          (embeddings only)
```

---

## Install Claude Desktop

1. Download and install from [claude.ai/download](https://claude.ai/download).
2. Sign in with your Anthropic account.
3. Open **Settings → Developer** and confirm MCP / developer options are available.

This guide covers **Claude Desktop only** — not the Anthropic API, Claude Code CLI, or Cursor.

---

## Start medical-mcp-server

### Option A — Docker Compose (recommended)

Requires Ollama on the host:

```bash
ollama pull nomic-embed-text:v1.5
docker compose up --build
```

### Option B — Local development

```bash
ollama pull nomic-embed-text:v1.5
mvn spring-boot:run
```

### Verify server health

```bash
curl http://localhost:8092/actuator/health
```

Wait for first-time dataset download and embedding pass (watch server logs). `search_cases` works after row load; `semantic_search` needs embeddings complete.

---

## Configure Claude Desktop

### Config file locations

| OS | Documented path |
|---|---|
| macOS | `~/Library/Application Support/Claude/claude_desktop_config.json` |
| Windows | `%APPDATA%\Claude\claude_desktop_config.json` |
| Linux | `~/.config/claude/claude_desktop_config.json` |

### In-app editor (recommended)

1. Open Claude Desktop.
2. Go to **Settings → Developer**.
3. Click **Edit Config** — opens `claude_desktop_config.json`.

### Add medical-mcp-server

Merge into `mcpServers` (keep any existing servers):

```json
{
  "mcpServers": {
    "medical-mcp-server": {
      "url": "http://localhost:8092/sse",
      "transport": "sse"
    }
  }
}
```

This matches [05-deployment.md](../05-deployment.md#mcp-client-connection) and [01-requirements.md §16](../01-requirements.md#16-mcp-client-connection).

### Apply changes

1. Save the JSON file (valid JSON only — no trailing commas).
2. **Fully quit** Claude Desktop (not just close the window).
3. Relaunch Claude Desktop.
4. Open a new chat and confirm the MCP connector icon shows `medical-mcp-server` connected.

---

## Windows MSIX note

Recent Claude Desktop builds on Windows use **MSIX packaging**. The **Edit Config** button may open a different file than the running app reads. If MCP servers do not appear after restart:

1. Use **Settings → Developer → Edit Config** first (simplest path).
2. If still missing, locate the virtualized config:

```powershell
Get-ChildItem -Path "$env:LOCALAPPDATA\Packages" -Filter "claude_desktop_config.json" -Recurse -ErrorAction SilentlyContinue
```

3. Edit the file under `Packages\...\LocalCache\...\Claude\` and restart again.

See [anthropics/claude-code#26073](https://github.com/anthropics/claude-code/issues/26073) for details.

---

## Connect in chat

1. Start a **new conversation**.
2. Confirm the tools/MCP indicator shows `medical-mcp-server` (hammer or plug icon, depending on version).
3. Ask naturally — Claude will call tools when needed.

### Context tips

- Medical **transcriptions are long** — start with `search_cases` + `limit=3`.
- Use `case-analysis` with `focus=description` before full `transcription`.
- For specialty filters, ask Claude to call `list_specialties` first for exact labels.

---

## Claude Desktop vs LM Studio

| | Claude Desktop | LM Studio |
|---|---|---|
| Model | Anthropic hosted | Local model on your GPU |
| Config file | `claude_desktop_config.json` | `mcp.json` |
| Resources | Strong MCP resource support | Varies by version |
| Best for | Daily use, W01 workflows | Offline / local model testing |

LM Studio setup: [lm-studio-mcp-manual-test.md](lm-studio-mcp-manual-test.md).

---

## Manual smoke checklist

Same checklist as [04-testing.md §11](../04-testing.md#11-manual-smoke-checklist-m7) and the LM Studio guide.

| Step | Suggested user message | Expected MCP call |
|---:|---|---|
| 1 | “What medical dataset stats are available?” | `get_dataset_stats` |
| 2 | “List all medical specialties and their case counts.” | `list_specialties` |
| 3 | “Search for cases about pacemaker interrogation.” | `search_cases` |
| 4 | “Find cases semantically similar to: pacemaker device check.” | `semantic_search` |
| 5 | “Get the full case details for UUID `<paste-from-step-3>`.” | `get_case` |
| 6 | “Use the case-analysis prompt for that case with focus transcription.” | `case-analysis` prompt |
| 7 | “Run case-analysis on the same case with focus specialty.” | `case-analysis` — includes `PREDICTED_LABEL` block |

### Pass criteria

- Steps 1–5 return non-empty dataset content.
- Step 6 includes a `Transcription:` section.
- Step 7 includes `PREDICTED_LABEL:` and ReAct-style instructions.

---

## Worked workflow — cardiology case (W01)

From [use-cases.md](../use-cases.md):

```
You: Find a cardiovascular case about cardiac catheterization, get the full
     transcription, and summarize it.

Expected tool chain:
  list_specialties (optional)
  → search_cases(query="cardiac catheterization",
                  specialty="Cardiovascular / Pulmonary", limit=3)
  → get_case(id="<uuid>")
  → case-analysis(caseId="<uuid>", focus="transcription")
```

---

## MCP resources

Claude Desktop supports reading MCP resources directly. Try:

- `medical://stats` — dataset snapshot (same as `get_dataset_stats`)
- `medical://cases/{uuid}` — full case JSON including `transcription`

Useful when you want case JSON attached without a separate `get_case` tool round-trip (UC-R01).

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| MCP server not listed after restart | Validate JSON; fully quit and relaunch; check Windows MSIX path |
| Connection refused on `:8092` | Start medical-mcp-server; verify health endpoint |
| Tools never invoked | Ask explicitly (“use the medical MCP tools to…”) |
| `semantic_search` empty | Start Ollama; wait for embedding pass in server logs |
| Wrong specialty filter | Use exact labels from `list_specialties` |
| 404 on MCP URL | Use `http://localhost:8092/sse` exactly |

### Logs

| OS | MCP logs directory |
|---|---|
| macOS | `~/Library/Logs/Claude/` |
| Windows | `%APPDATA%\Claude\logs\` (or MSIX equivalent) |

Look for `mcp-server-medical-mcp-server.log` and `mcp.log`.

---

## Security

- Default setup is for **local development**. Do not expose port 8092 publicly without reviewing [SecurityConfig](../../src/main/java/com/example/medicalmcp/core/config/SecurityConfig.java).
- No API key is required in `claude_desktop_config.json` for this server (default profile).

---

## Related documentation

- [mcp-user-guide.md](mcp-user-guide.md) — tool and prompt reference
- [lm-studio-mcp-manual-test.md](lm-studio-mcp-manual-test.md) — local-model alternative
- [05-deployment.md](../05-deployment.md) — Docker, env vars
- [prompt-lab-user-guide.md](prompt-lab-user-guide.md) — optional eval profile
