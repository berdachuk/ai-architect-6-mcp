# medicalcase module — agent guide

**Package:** `com.example.medicalmcp.medicalcase`  
**Modulith deps:** `core` only

## Purpose

Domain records and `MedicalCaseRepository` — persistence boundary for the `medical_case` table.

## Owned domain models

| Record | Role |
|---|---|
| `MedicalCase` | Full case row (incl. UUID, split, embedding) |
| `CaseSummary` | Lightweight search result body |
| `SemanticMatch` | `CaseSummary` + similarity score |
| `SpecialtyCount` | Specialty aggregation |
| `DatasetStats` | Dataset statistics snapshot |

Schema: `docs/01-requirements.md` §5 · Records: `docs/03-design.md`

## Layout

```text
medicalcase/
├── domain/           # Java records — no Spring stereotypes
├── repository/       # MedicalCaseRepository interface
└── repository/impl/  # MedicalCaseRepositoryImpl (@Repository)
```

## Constraints

- No dependency on `mcp`, `retrieval`, `embedding`, `dataset`
- Repository interface is the only persistence API for other modules
- 24-char hex ID assigned at insert — not from HuggingFace

## Skills

- `.agents/skills/domain-modeling/SKILL.md`
- `.agents/skills/db-migrations/SKILL.md`

## Tests

- Repository integration tests (M2–M3)
- Domain record mapping unit tests
