# Module locks

`locks/<module>.md` records which agent/branch currently owns a module. **Advisory but enforced** by the `write-less-code` and `security-check` skills.

## When to acquire a lock

Before editing any **coupled file pair** — two or more files that must change in lockstep. Without a lock, a green merge can silently break the lockstep (the M130-class risk): one agent edits the prompt template, another edits the sanitizer that parses it, and both merge cleanly while the pair is now inconsistent.

## Format

```markdown
# Lock: <module>

| Field | Value |
|---|---|
| Module | <module> |
| Held by | <branch or agent id> |
| Acquired | <YYYY-MM-DD> |
| Expires | <YYYY-MM-DD> (default +7 days) |
| Coupled files | <list of files that must change together> |
| Reason | <why lockstep editing is required> |
```

Set `held-by: released` (or delete the file) when the workstream merges.

## Coupled file pairs (known)

| Module | Files that must change together | Why |
|---|---|---|
| `mcp` | `MedicalCasePrompts` prompt template + `core/prompt/PromotedSpecialtyClassificationInstructions` | `focus=specialty` injects the promoted block; changing one without the other breaks the template |
| `medicalcase` | `MedicalCase` record + `V*__*.sql` migration + `MedicalCaseRepositoryImpl` | Adding/removing a column touches all three |
| `retrieval` | `VectorSearchService` SQL file (`@InjectSql`) + `retrieval/AGENTS.md` quality notes | SQL bind names must match `MapSqlParameterSource` keys |

## Acquisition rules

1. Before editing a coupled pair, read `locks/<module>.md`.
2. If no lock exists, or the existing lock is `released`/expired, create/overwrite it with your branch.
3. If a non-expired lock exists for a **different** branch: do **not** edit the coupled files. Either pick a different module, or coordinate with the holding branch.
4. On merge, release the lock (delete the file or set `held-by: released`).
5. Locks are advisory at the git level but enforced by skills — ignoring a live lock recreates silent semantic conflicts.