# Manual MCP testing with LM Studio

**Version:** 1.0  
**LM Studio:** ≥ 0.3.17 ([MCP documentation](https://lmstudio.ai/docs/app/mcp))  
**MCP server:** `http://localhost:8092/sse`

Step-by-step guide to connect **LM Studio** as an MCP host to **medical-mcp-server** and manually exercise all tools and the `case-analysis` prompt.

---

## Overview

| Component | Role |
|---|---|
| **LM Studio** | MCP host — local chat model + tool calling |
| **medical-mcp-server** | Remote SSE MCP server — dataset tools on port 8092 |
| **PostgreSQL + pgvector** | Case storage and vector index |
| **Ollama** | Embedding API for `semantic_search` only (`nomic-embed-text:v1.5`) |

LM Studio does **not** replace Ollama for embeddings. The MCP server calls Ollama independently when you use `semantic_search`. See [01-requirements.md §3](../01-requirements.md#3-embedding-model).

### Architecture

```text
┌─────────────────┐     MCP/SSE      ┌──────────────────────────┐
│   LM Studio     │ ───────────────► │  medical-mcp-server:8092 │
│  (chat model)   │                  └────────────┬─────────────┘
└─────────────────┘                               │
                                    ┌─────────────┴─────────────┐
                                    ▼                           ▼
                            PostgreSQL + pgvector            Ollama :11434
                                                         (embeddings only)
```

---

## Install LM Studio

1. Install [LM Studio](https://lmstudio.ai/) **0.3.17 or newer**.
2. Open **Developer** settings.
3. Enable **Allow remote MCP** (required for `url`-based servers pointing to `localhost`).

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
# Postgres with pgvector on :5432
ollama pull nomic-embed-text:v1.5
mvn spring-boot:run
```

### Verify server health

```bash
curl http://localhost:8092/actuator/health
```

Wait for first-time dataset download and embedding pass to finish (watch server logs). `search_cases` works after row load; `semantic_search` needs embeddings complete.

---

## Configure LM Studio `mcp.json`

### File location

| OS | Path |
|---|---|
| Windows | `%USERPROFILE%\.lmstudio\mcp.json` |
| macOS / Linux | `~/.lmstudio/mcp.json` |

### In-app editor

1. Open LM Studio.
2. Switch to the **Program** tab (right sidebar).
3. Click **Install → Edit mcp.json**.

### Add medical-mcp-server

```json
{
  "mcpServers": {
    "medical-mcp-server": {
      "url": "http://localhost:8092/sse"
    }
  }
}
```

Save the file. LM Studio reloads MCP servers automatically.

**Endpoints (from server config):**

| Path | Purpose |
|---|---|
| `/sse` | SSE transport (use this in `mcp.json`) |
| `/mcp/message` | Message endpoint (handled by client library) |

Do **not** use `/mcp` alone — you will get 404.

Compare with Claude Desktop config in [05-deployment.md](../05-deployment.md#mcp-client-connection).

---

## Connect in chat

1. Start a **new chat** in LM Studio.
2. Select a local model with enough **context length** (transcriptions are long).
3. Enable **MCP / tools** for the session (per current LM Studio UI).
4. Confirm `medical-mcp-server` appears in the connected MCP server list.

### Token tips

- Start with small `limit` / `topK` (e.g. 3–5).
- Prefer `focus=description` before full `transcription` prompts.
- Large tool JSON payloads can overflow smaller models — reduce result counts if the model loses coherence.

---

## Manual smoke checklist

Mirror of [04-testing.md §11](../04-testing.md#11-manual-smoke-checklist-m7). Send natural-language prompts; the model should call the listed MCP capability.

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

- Steps 1–5 return non-empty, plausible dataset content.
- Step 6 includes a `Transcription:` section.
- Step 7 includes `PREDICTED_LABEL:` and ReAct-style instructions.

---

## Resources (optional)

If your LM Studio build exposes MCP resources in the UI, try:

- `medical://stats`
- `medical://cases/{uuid}`

These mirror `get_dataset_stats` and `get_case` JSON.

---

## API testing note

LM Studio’s chat UI is the **primary** path for this guide.

The REST `/v1/responses` API may require an explicit `server_url` for remote MCP in some versions and might not read `mcp.json` the same way as the UI ([lmstudio-bug-tracker #1073](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/1073)).

**Alternatives for automated smoke:**

```bash
mvn verify -Pe2e          # McpSyncClient SSE integration test
mvn verify -Pintegration  # Full Testcontainers contract tests
```

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| MCP server not in LM Studio list | Validate JSON syntax; restart LM Studio |
| Connection refused on `:8092` | Start server; check `SERVER_PORT` |
| `semantic_search` always empty | Start Ollama; wait for embedding pass in logs |
| Model stops mid-response | Reduce `limit`/`topK`; use shorter `focus` |
| 404 on MCP URL | Use `http://localhost:8092/sse` exactly |
| Wrong specialty filter results | Use exact labels from step 2 (`list_specialties`) |

---

## Security

- Default setup is for **local development**. Do not expose port 8092 to the internet without reviewing [SecurityConfig](../../src/main/java/com/example/medicalmcp/core/config/SecurityConfig.java).
- This server needs **no API key** in `mcp.json` for the default profile.

---

## Related documentation

- [mcp-user-guide.md](mcp-user-guide.md) — tool and prompt reference
- [05-deployment.md](../05-deployment.md) — Docker, env vars, Claude Desktop
- [prompt-lab-user-guide.md](prompt-lab-user-guide.md) — optional eval profile (not used in this smoke test)
