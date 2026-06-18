# MCP Self-Description Improvements

**Problem:** Generic chat clients (like ai-chat) don't know how to compose a meaningful multi-tool workflow because MCP tools are discovered as isolated functions. The client sees 5 tools, a prompt, and 2 resources — but has no idea about the *intended usage flow* (search → get → analyze).

**Solution:** Use two standard MCP protocol features that are currently unused:

1. **`instructions`** — Server-level narrative describing the tools, their relationships, and the recommended workflow.
2. **`prompt` descriptions + argument schemas** — Already partially supported; ensure prompts expose their argument contract clearly.

---

## 1. MCP Server Side (`ai-architect-6-mcp`)

### 1.1 Set `instructions` on initialization

The MCP protocol defines an `instructions` field on `InitializeResult`. Set it in `WebMvcSseServerTransportProvider` configuration:

```java
@Bean
public WebMvcSseServerTransportProvider mcpTransport() {
    return WebMvcSseServerTransportProvider.builder()
            .messageEndpoint("/mcp/message")
            .serverInfo(new McpSchema.Implementation("medical-mcp-server", "1.6.0"))
            .serverCapabilities(ServerCapabilities.builder()
                    .tools(true)
                    .resources(true)
                    .prompts(true)
                    .build())
            .serverInstructions("""
                    ## Workflow

                    ### Step 1 — Explore the dataset
                    Ask about available stats or specialties.

                    ### Step 2 — Search for cases
                    Use `search_cases` (keyword/FTS) or `semantic_search` (vector similarity) to find relevant cases.

                    ### Step 3 — Retrieve full details
                    Use `get_case` with a UUID from Step 2 to get the full transcription.

                    ### Step 4 — Analyze with prompt
                    Use `case-analysis` prompt with a UUID and optionally `focus` parameter.

                    ### Important
                    - `get_case` requires a valid UUID (`id` parameter). Obtain it from `search_cases` or `semantic_search` first.
                    - `case-analysis` prompt also requires a valid UUID (`caseId`). The `focus` parameter controls which fields are emphasized.
                    """)
            .build();
}
```

### 1.2 Improve tool descriptions

Update `MedicalCaseTools.java` annotations to hint at workflow ordering:

```java
@McpTool(
    name = "search_cases",
    description = "[Step 2] Full-text search over medical case transcriptions, descriptions, and keywords. Returns case IDs (UUIDs) that can be used with get_case and case-analysis prompt.",
    ...
)
```

```java
@McpTool(
    name = "get_case",
    description = "[Step 3] Retrieve a single medical case by UUID, including the full transcription text. Requires a UUID obtained from search_cases or semantic_search.",
    ...
)
```

```java
@McpTool(
    name = "get_dataset_stats",
    description = "[Step 1] Return dataset statistics: total cases, breakdown by specialty and by split (train/validation/test). Use first to understand what data is available.",
    ...
)
```

### 1.3 Improve prompt description

```java
@McpPrompt(
    name = "case-analysis",
    description = "[Step 4] Structured prompt for LLM analysis of a medical case. Requires a case UUID (from search_cases/semantic_search). Optional focus: description | transcription | keywords | specialty | all. When focus=specialty includes PREDICTED_LABEL classification block.",
)
```

---

## 2. ai-chat Client Side

### 2.1 Read `instructions` from MCP server

In `McpClientConnector.java`, capture `client.getServerInstructions()` and store it in `McpServerInfo`:

```java
String instructions = client.getServerInstructions();
```

Add field to `McpServerInfo` record:

```java
public record McpServerInfo(
        String connectionId,
        String connectionName,
        McpSyncClient client,
        String serverName,
        String version,
        String url,
        ServerStatus status,
        String downReason,
        List<McpSchema.Tool> tools,
        List<McpSchema.Resource> resources,
        List<McpSchema.Prompt> prompts,
        String instructions) {}
```

### 2.2 Include instructions in catalog text

In `McpServerRegistry.formatServerCatalog()`, prepend server instructions before the tool list:

```java
private String formatServerCatalog(McpServerInfo server) {
    StringBuilder builder = new StringBuilder();
    builder.append("### ").append(server.connectionName());
    if (server.serverName() != null && !server.serverName().isBlank()) {
        builder.append(" (").append(server.serverName()).append(")");
    }
    builder.append("\n");
    if (server.instructions() != null && !server.instructions().isBlank()) {
        builder.append(server.instructions()).append("\n\n");
    }
    // ... tool listing with inputSchema as already implemented ...
}
```

### 2.3 Expose prompts in catalog text

The current catalog only lists tools. Also list available prompts with their parameters so the LLM knows about `case-analysis`:

```java
private String formatServerCatalog(McpServerInfo server) {
    // ... tool listing ...
    if (!server.prompts().isEmpty()) {
        builder.append("\n#### Prompts\n");
        server.prompts().forEach(prompt -> {
            builder.append("- **").append(prompt.name()).append("**: ");
            builder.append(prompt.description() == null ? "" : prompt.description()).append("\n");
            prompt.arguments().forEach(arg -> {
                builder.append("  - ").append(arg.name());
                if (arg.required() != null && arg.required()) {
                    builder.append(" (required)");
                }
                if (arg.description() != null) {
                    builder.append(": ").append(arg.description());
                }
                builder.append("\n");
            });
        });
    }
    return builder.toString().trim();
}
```

---

## 3. Result

After both sides are updated, a generic chat client will see:

```markdown
### medical-dataset (medical-mcp-server)
## Workflow

### Step 1 — Explore the dataset
Ask about available stats or specialties.

### Step 2 — Search for cases
Use `search_cases` (keyword/FTS) or `semantic_search` (vector similarity) to find relevant cases.

### Step 3 — Retrieve full details
Use `get_case` with a UUID from Step 2 to get the full transcription.

### Step 4 — Analyze with prompt
Use `case-analysis` prompt with a UUID and optionally `focus` parameter.

### Important
- `get_case` requires a valid UUID (`id` parameter). Obtain it from `search_cases` or `semantic_search` first.
- `case-analysis` prompt also requires a valid UUID (`caseId`). The `focus` parameter controls which fields are emphasized.

- **[Step 1] get_dataset_stats**: Return dataset statistics...
  - Parameters: none
- **[Step 1] list_specialties**: List all medical specialties...
  - Parameters: none
- **[Step 2] search_cases**: Full-text search...
  - Parameters: query (string, required), specialty (string), split (string), limit (integer)
- **[Step 2] semantic_search**: Vector similarity search...
  - Parameters: query (string, required), specialty (string), topK (integer), minSimilarity (number)
- **[Step 3] get_case**: Retrieve a single medical case by UUID...
  - Parameters: id (string, required)
- **[Step 4] get_dataset_stats**: Get dataset stats...

#### Prompts
- **case-analysis**: [Step 4] Structured prompt for LLM analysis...
  - caseId (required): Server UUID from search_cases / semantic_search
  - focus: Dataset field emphasis: description | transcription | keywords | specialty | all
```

This is fully protocol-compliant, works with any MCP client, and requires no ai-chat-specific hacks.
