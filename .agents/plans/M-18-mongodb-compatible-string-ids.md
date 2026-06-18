# M-18 — Switch IDs from UUID to MongoDB-compatible strings

**Status:** ⬜ Active  
**Depends on:** M1–M8, M-17  
**Database migration:** Not required — new database from scratch

## Problem

Current `id` column is `UUID PRIMARY KEY DEFAULT gen_random_uuid()`. UUIDs are 36-char strings with dashes (`34409f81-20fb-4c5e-a058-b43b8d31d702`) — verbose in MCP tool output, not compatible with MongoDB-style systems, and harder to copy/paste in chat UIs.

## Solution

Switch to MongoDB ObjectId-compatible 24-char hex strings (e.g. `507f1f77bcf86cd799439011`). Shorter, no dashes, copy-friendly, and compatible with document-store conventions.

## Scope

| Layer | Files to change |
|---|---|
| Flyway | `V1__init_medical_cases.sql` — `id TEXT PRIMARY KEY`, remove `DEFAULT gen_random_uuid()` |
| Domain records | `MedicalCase`, `CaseSummary` — `UUID id` → `String id` |
| Repository API | `MedicalCaseRepository` — `findById(UUID)` → `findById(String)`, `Map<UUID, float[]>` → `Map<String, float[]>` |
| Repository impl | `MedicalCaseRepositoryImpl` — all SQL bindings |
| SQL files | `selectById.sql`, `insert.sql`, `updateEmbedding.sql`, `findWithoutEmbeddingsBySplit.sql`, `fullTextSearch.sql`, `semanticSearch.sql` |
| Dataset loader | `DatasetLoaderServiceImpl` — generate 24-char hex IDs instead of `UUID.randomUUID()` |
| Core utility | `UuidUtils` → `IdUtils` — validate 24-char hex strings |
| MCP tools | `MedicalCaseTools` — `parseUuid` → `IdUtils.isValidId`, update param descriptions |
| MCP resources | `MedicalCaseResources` — `findCase` uses `IdUtils` |
| MCP prompts | `MedicalCasePrompts` — `caseId` param description |
| Tests | All fixtures, `MedicalCasePromptsStructureTest`, integration tests |

## Implementation steps

### 1. Flyway — `V1__init_medical_cases.sql`

```sql
id TEXT PRIMARY KEY,   -- MongoDB ObjectId-compatible 24-char hex
```

Remove `DEFAULT gen_random_uuid()`. ID generation moves to the loader.

### 2. ID generation — `DatasetLoaderServiceImpl`

```java
import org.bson.types.ObjectId;

String id = new ObjectId().toHexString();  // 24-char hex
```

Add `org.mongodb:bson` dependency (lightweight, no driver needed).

### 3. Domain records

```java
public record MedicalCase(
    String id,           // was UUID
    ...
) {}

public record CaseSummary(
    String id,           // was UUID
    ...
) {}
```

### 4. Repository

```java
public interface MedicalCaseRepository {
    Optional<MedicalCase> findById(String id);       // was UUID
    void updateEmbeddingsBatch(Map<String, float[]> embeddings);  // was Map<UUID, float[]>
    ...
}
```

### 5. Core utility — `IdUtils` (rename from `UuidUtils`)

```java
public final class IdUtils {
    private static final Pattern HEX24 = Pattern.compile("^[a-f0-9]{24}$");

    public static boolean isValidId(String id) {
        return id != null && HEX24.matcher(id.trim().toLowerCase()).matches();
    }
}
```

### 6. MCP tools — `MedicalCaseTools`

```java
@McpToolParam(description = "Case ID (24-char hex string)", required = true) String id

// In getCase:
if (!IdUtils.isValidId(id)) { return null; }
caseRepository.findById(id.trim().toLowerCase()).orElse(null);
```

### 7. MCP prompts — `MedicalCasePrompts`

```java
@McpArg(name = "caseId", description = "Case ID (24-char hex string) from search_cases / semantic_search", required = true)
```

### 8. SQL files — all `:id` bindings remain, but type changes from UUID to TEXT

No SQL syntax changes needed — `WHERE id = :id` works for both UUID and TEXT. Only the DDL changes.

### 9. Tests

- Update all test fixtures to use 24-char hex IDs
- `MedicalCasePromptsStructureTest` — update ID assertions
- Integration tests — update UUID references

## Dependency

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>bson</artifactId>
    <version>5.4.0</version>
</dependency>
```

## Files affected (estimated)

| File | Change |
|---|---|
| `pom.xml` | Add `bson` dependency |
| `V1__init_medical_cases.sql` | `id UUID` → `id TEXT`, remove `DEFAULT` |
| `MedicalCase.java` | `UUID id` → `String id` |
| `CaseSummary.java` | `UUID id` → `String id` |
| `MedicalCaseRepository.java` | `UUID` → `String` in signatures |
| `MedicalCaseRepositoryImpl.java` | All `UUID` → `String` in method bodies |
| `DatasetLoaderServiceImpl.java` | `UUID.randomUUID()` → `new ObjectId().toHexString()` |
| `UuidUtils.java` → `IdUtils.java` | Rename, change validation |
| `MedicalCaseTools.java` | `UuidUtils.parseUuid` → `IdUtils.isValidId` |
| `MedicalCaseResources.java` | `UUID.fromString` → `IdUtils.isValidId` |
| `MedicalCasePrompts.java` | `UuidUtils.parseUuid` → `IdUtils.isValidId`, param desc |
| `insert.sql` | Remove `:id` from INSERT (generated in Java now) |
| `selectById.sql` | No change (`:id` binding works for TEXT) |
| `updateEmbedding.sql` | No change |
| `findWithoutEmbeddingsBySplit.sql` | No change |
| `fullTextSearch.sql` | No change |
| `semanticSearch.sql` | No change |
| All test fixtures | Replace UUIDs with 24-char hex strings |
| `MedicalCasePromptsStructureTest` | Update ID assertions |
| Integration tests | Update UUID references |

## Acceptance criteria

- [ ] `mvn test` — all 21 tests pass with string IDs
- [ ] `mvn verify -Pintegration` — Testcontainers IT pass
- [ ] `mvn verify -Pe2e` — SSE smoke test pass
- [ ] `get_case` returns case with 24-char hex `id` field
- [ ] `search_cases` / `semantic_search` return `CaseSummary` with 24-char hex `id`
- [ ] `case-analysis` prompt accepts 24-char hex `caseId`
- [ ] Invalid IDs (UUID format, short strings, non-hex) rejected with null/empty response
