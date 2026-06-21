#!/usr/bin/env bash
# sync-memory-index.sh — regenerate generated memory-bank index files.
#
# Sources (append-only, hand-authored):
#   .agents/memory-bank/registry/*.jsonl     — stable IDs (one JSON per line)
#   .agents/memory-bank/records/**/*.md      — per-record long-form files
#
# Generated (read-only to agents; do not hand-edit):
#   .agents/memory-bank/activeContext.md
#   .agents/memory-bank/progress.md
#   .agents/memory-bank/decisions.md
#   .agents/memory-bank/productContext.md     (traceability tables only)
#   .agents/plans/00-index.md
#
# Usage:
#   scripts/sync-memory-index.sh           # regenerate
#   scripts/sync-memory-index.sh --check   # exit 1 if generated files are stale (CI gate)
#
# Requires: bash 4+, python3 (stdlib only). No external deps.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MB="$ROOT/.agents/memory-bank"
REG="$MB/registry"
REC="$MB/records"
PLANS="$ROOT/.agents/plans"
CHECK=0
[[ "${1:-}" == "--check" ]] && CHECK=1

# ---- helpers ---------------------------------------------------------------

json_field() { # <line> <key>  → prints string value (no quotes)
  python3 -c '
import sys, json
line, key = sys.argv[1], sys.argv[2]
obj = json.loads(line)
v = obj.get(key, "")
if isinstance(v, list):
    print(",".join(v))
else:
    print(v)
' "$1" "$2"
}

read_jsonl() { # <file>  → prints each valid non-empty line
  [[ -f "$1" ]] || return 0
  python3 -c '
import sys, json
path = sys.argv[1]
with open(path) as f:
    for i, line in enumerate(f, 1):
        line = line.strip()
        if not line:
            continue
        try:
            json.loads(line)
        except Exception as e:
            print(f"ERROR: invalid JSON in {path} line {i}: {e}", file=sys.stderr)
            sys.exit(1)
        print(line)
' "$1"
}

# Capture generated files in memory, compare to disk on --check.
declare -A GENERATED

stage() { # <path> <content>
  GENERATED["$1"]="$2"
}

flush() {
  local tmpdir
  tmpdir="$(mktemp -d)"
  local rc=0
  for path in "${!GENERATED[@]}"; do
    local dir
    dir="$(dirname "$path")"
    mkdir -p "$dir"
    if [[ $CHECK -eq 1 ]]; then
      printf '%s' "${GENERATED[$path]}" > "$tmpdir/new"
      if [[ -f "$path" ]] && diff -q "$path" "$tmpdir/new" >/dev/null 2>&1; then
        : # in sync
      else
        echo "STALE: $path" >&2
        rc=1
      fi
    else
      printf '%s' "${GENERATED[$path]}" > "$path"
    fi
  done
  rm -rf "$tmpdir"
  if [[ $rc -ne 0 ]]; then
    echo "Run scripts/sync-memory-index.sh to regenerate." >&2
  fi
  return $rc
}

# ---- activeContext.md ------------------------------------------------------

gen_active_context() {
  local out=""
  out+="# Active context"$'\n'
  out+=$'\n'"**Generated** by \`scripts/sync-memory-index.sh\` — do not hand-edit. Edit \`records/active/*.md\` and re-run."$'\n'
  out+=$'\n'"## Current focus"$'\n'

  local active_files=()
  while IFS= read -r f; do
    active_files+=("$f")
  done < <(cd "$REC/active" 2>/dev/null && ls *.md 2>/dev/null | sort || true)

  if [[ ${#active_files[@]} -eq 0 ]]; then
    out+="No active milestones."$'\n'
  else
    for f in "${active_files[@]}"; do
      local title plan_path
      title="$(grep -m1 '^# ' "$REC/active/$f" | sed 's/^# //')"
      # extract the plan filename from the "| Plan | [text](path) |" row
      plan_path="$(grep -m1 '| Plan |' "$REC/active/$f" | sed -n 's/.*(\([^)]*\)).*/\1/p' | sed 's#.*/##')"
      out+="- **$title** — [record](records/active/$f)"
      [[ -n "$plan_path" ]] && out+=" · [plan](../plans/$plan_path)"
      out+=$'\n'
    done
  fi

  out+=$'\n'"## Active risks"$'\n'
  local risk_count=0
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    local id summary severity
    id="$(json_field "$line" id)"
    summary="$(json_field "$line" summary)"
    severity="$(json_field "$line" severity)"
    out+="| $id | $severity | $summary |"$'\n'
    risk_count=$((risk_count+1))
  done < <(read_jsonl "$REG/risk.jsonl")
  [[ $risk_count -eq 0 ]] && out+="_None._"$'\n'

  out+=$'\n'"## Open scenarios (BDD)"$'\n'
  local scn_count=0
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    local id feature
    id="$(json_field "$line" id)"
    feature="$(json_field "$line" feature)"
    out+="- **$id** — $feature"$'\n'
    scn_count=$((scn_count+1))
  done < <(read_jsonl "$REG/scn.jsonl")
  [[ $scn_count -eq 0 ]] && out+="_None._"$'\n'

  out+=$'\n'"## Reference files (hand-edit these)"$'\n'
  out+="- [projectbrief.md](projectbrief.md)"$'\n'
  out+="- [systemPatterns.md](systemPatterns.md)"$'\n'
  out+="- [techContext.md](techContext.md)"$'\n'
  out+="- [productContext.md](productContext.md) (prose hand-edited; tables generated)"$'\n'

  stage "$MB/activeContext.md" "$out"
}

# ---- progress.md -----------------------------------------------------------

gen_progress() {
  local out=""
  out+="# Progress log"$'\n'
  out+=$'\n'"**Generated** by \`scripts/sync-memory-index.sh\` — do not hand-edit. One \`records/progress/M{NN}.md\` per milestone."$'\n'

  local progress_files=()
  while IFS= read -r f; do
    progress_files+=("$f")
  done < <(cd "$REC/progress" 2>/dev/null && ls *.md 2>/dev/null | sort || true)

  for f in "${progress_files[@]}"; do
    local title
    title="$(grep -m1 '^# ' "$REC/progress/$f" | sed 's/^# //')"
    out+=$'\n'"## $title"$'\n'
    # include the summary table rows (lines starting with |) and a link.
    # Rewrite record-relative plan links (../../../plans/) to progress.md depth (../plans/).
    while IFS= read -r row; do
      row="${row//..\/..\/..\/plans\//..\/plans\/}"
      out+="$row"$'\n'
    done < <(grep '^|' "$REC/progress/$f" | grep -v '^| ---' || true)
    out+="[full record](records/progress/$f)"$'\n'
  done

  out+=$'\n'"## Milestone status"$'\n'
  out+="| Milestone | Status |"$'\n'
  out+="|---|---|"$'\n'
  # completed
  for f in "${progress_files[@]}"; do
    local title status
    title="$(grep -m1 '^# ' "$REC/progress/$f" | sed 's/^# //')"
    status="$(grep -m1 'Status |' "$REC/progress/$f" | sed 's/.*| //; s/ |.*//')"
    out+="| $title | $status |"$'\n'
  done
  # active
  while IFS= read -r f; do
    local title status
    title="$(grep -m1 '^# ' "$REC/active/$f" | sed 's/^# //')"
    status="$(grep -m1 'Status |' "$REC/active/$f" | sed 's/.*| //; s/ |.*//')"
    out+="| $title | $status |"$'\n'
  done < <(cd "$REC/active" 2>/dev/null && ls *.md 2>/dev/null | sort || true)

  out+=$'\n'"Canonical milestone table: [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones)"$'\n'

  stage "$MB/progress.md" "$out"
}

# ---- decisions.md ----------------------------------------------------------

gen_decisions() {
  local out=""
  out+="# Decisions"$'\n'
  out+=$'\n'"**Generated** by \`scripts/sync-memory-index.sh\` — do not hand-edit. Index rows from \`registry/dec.jsonl\`; bodies in \`records/decisions/DEC-###.md\`."$'\n'
  out+=$'\n'"ADR-style log. Full rationale in linked records/docs where applicable."$'\n'

  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    local id title status date modules rationale body
    id="$(json_field "$line" id)"
    title="$(json_field "$line" title)"
    status="$(json_field "$line" status)"
    date="$(json_field "$line" date)"
    modules="$(json_field "$line" modules)"
    rationale="$(json_field "$line" rationale)"
    body="$(json_field "$line" body)"
    out+=$'\n'"## $id — $title"$'\n'
    out+=$'\n'"| Field | Value |"$'\n'
    out+="|---|---|"$'\n'
    out+="| Status | $status |"$'\n'
    out+="| Date | $date |"$'\n'
    [[ -n "$modules" ]] && out+="| Modules | $modules |"$'\n'
    out+="| Rationale | $rationale |"$'\n'
    [[ -n "$body" ]] && out+="| Body | [$body]($body) |"$'\n'
  done < <(read_jsonl "$REG/dec.jsonl")

  stage "$MB/decisions.md" "$out"
}

# ---- productContext.md tables ----------------------------------------------

gen_product_context() {
  local out=""
  # Preserve prose by reading existing file's prose up to the generated marker.
  local existing=""
  [[ -f "$MB/productContext.md" ]] && existing="$(cat "$MB/productContext.md")"
  local prose=""
  if [[ "$existing" == *"<!-- BEGIN GENERATED -->"* ]]; then
    prose="${existing%%<!-- BEGIN GENERATED -->*}"
  elif [[ -n "$existing" ]]; then
    prose="$existing"
  fi
  # Normalize: strip trailing whitespace, then add exactly one blank line before the marker.
  prose="$(printf '%s' "$prose" | sed -e 's/[[:space:]]*$//')"
  out+="$prose"$'\n'
  out+=$'\n'"<!-- BEGIN GENERATED -->"$'\n'
  out+="<!-- Generated by scripts/sync-memory-index.sh — do not edit below this line. -->"$'\n'
  out+=$'\n'"## Traceability (generated)"$'\n'

  out+=$'\n'"### Requirements → modules"$'\n'
  out+="| ID | Summary | Module | Domain models |"$'\n'
  out+="|---|---|---|---|"$'\n'
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    local id summary module dm
    id="$(json_field "$line" id)"
    summary="$(json_field "$line" summary)"
    module="$(json_field "$line" module)"
    dm="$(json_field "$line" domain_models)"
    out+="| $id | $summary | $module | $dm |"$'\n'
  done < <(read_jsonl "$REG/req.jsonl")

  out+=$'\n'"### Non-functional requirements"$'\n'
  out+="| ID | Metric | Target |"$'\n'
  out+="|---|---|---|"$'\n'
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    local id metric target
    id="$(json_field "$line" id)"
    metric="$(json_field "$line" metric)"
    target="$(json_field "$line" target)"
    out+="| $id | $metric | $target |"$'\n'
  done < <(read_jsonl "$REG/nfr.jsonl")

  out+=$'\n'"### Scenarios → requirements"$'\n'
  out+="| ID | Feature | Module | Req IDs |"$'\n'
  out+="|---|---|---|---|"$'\n'
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    local id feature module req_ids
    id="$(json_field "$line" id)"
    feature="$(json_field "$line" feature)"
    module="$(json_field "$line" module)"
    req_ids="$(json_field "$line" req_ids)"
    out+="| $id | $feature | $module | $req_ids |"$'\n'
  done < <(read_jsonl "$REG/scn.jsonl")

  out+=$'\n'"### Tests → requirements"$'\n'
  out+="| ID | Class | Module | Profile | Req IDs |"$'\n'
  out+="|---|---|---|---|---|"$'\n'
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    local id class module profile req_ids
    id="$(json_field "$line" id)"
    class="$(json_field "$line" class)"
    module="$(json_field "$line" module)"
    profile="$(json_field "$line" profile)"
    req_ids="$(json_field "$line" req_ids)"
    out+="| $id | $class | $module | $profile | $req_ids |"$'\n'
  done < <(read_jsonl "$REG/test.jsonl")

  out+=$'\n'"<!-- END GENERATED -->"$'\n'

  stage "$MB/productContext.md" "$out"
}

# ---- plans/00-index.md -----------------------------------------------------

gen_plans_index() {
  local out=""
  out+="# Plans index"$'\n'
  out+=$'\n'"**Generated** by \`scripts/sync-memory-index.sh\` — do not hand-edit. Canonical milestone table: [docs/01-requirements.md §14](../../docs/01-requirements.md#14-milestones)."$'\n'

  out+=$'\n'"## Active"$'\n'
  out+="| Plan | Milestone | Status |"$'\n'
  out+="|---|---|---|"$'\n'
  local has_active=0
  while IFS= read -r f; do
    [[ -z "$f" ]] && continue
    local title
    title="$(grep -m1 '^# ' "$REC/active/$f" | sed 's/^# //')"
    out+="| [record](../memory-bank/records/active/$f) | $title | ⬜ Active |"$'\n'
    has_active=1
  done < <(cd "$REC/active" 2>/dev/null && ls *.md 2>/dev/null | sort || true)
  [[ $has_active -eq 0 ]] && out+="| _none_ | | |"$'\n'

  out+=$'\n'"## Deferred"$'\n'
  out+="| Plan | Milestone |"$'\n'
  out+="|---|---|"$'\n'
  local has_deferred=0
  while IFS= read -r f; do
    [[ -z "$f" ]] && continue
    local title
    title="$(grep -m1 '^# ' "$REC/deferred/$f" 2>/dev/null | sed 's/^# //' || true)"
    out+="| [record](../memory-bank/records/deferred/$f) | $title |"$'\n'
    has_deferred=1
  done < <(cd "$REC/deferred" 2>/dev/null && ls *.md 2>/dev/null | sort || true)
  [[ $has_deferred -eq 0 ]] && out+="| _none_ | |"$'\n'

  out+=$'\n'"## Completed"$'\n'
  out+="| Plan | Milestone | Record |"$'\n'
  out+="|---|---|---|"$'\n'
  while IFS= read -r f; do
    [[ -z "$f" ]] && continue
    local title plan_text plan_path
    title="$(grep -m1 '^# ' "$REC/progress/$f" | sed 's/^# //')"
    # extract link text and path from "| Plan | [text](path) |"
    plan_text="$(grep -m1 '| Plan |' "$REC/progress/$f" | sed -n 's/.*\[\([^]]*\)\].*/\1/p')"
    plan_path="$(grep -m1 '| Plan |' "$REC/progress/$f" | sed -n 's/.*(\([^)]*\)).*/\1/p' | sed 's#.*/##')"
    if [[ -n "$plan_path" ]]; then
      out+="| [$plan_text](archive/$plan_path) | $title | [record](../memory-bank/records/progress/$f) |"$'\n'
    else
      out+="| $f | $title | [record](../memory-bank/records/progress/$f) |"$'\n'
    fi
  done < <(cd "$REC/progress" 2>/dev/null && ls *.md 2>/dev/null | sort || true)

  out+=$'\n'"## Conventions"$'\n'
  out+="- Filename: \`M-NN-short-topic.md\` (zero-padded \`NN\`; plan \`M-0N\` maps to requirements milestone \`M(N-1)\` for M1+)"$'\n'
  out+="- Link requirement sections and test deliverables from [docs/04-testing.md §10](../../docs/04-testing.md#10-mapping-to-milestones)"$'\n'
  out+="- Update \`records/progress/M{NN}.md\` when a plan completes; run \`scripts/sync-memory-index.sh\` to refresh this index."$'\n'
  out+=$'\n'"## SQL conventions (DEC-010, DEC-011)"$'\n'
  out+="- Files: \`src/main/resources/sql/{module}/*.sql\`"$'\n'
  out+="- Injection: \`@InjectSql(\"/sql/medicalcase/findById.sql\") String findByIdSql;\` in \`*/repository/impl/*\`"$'\n'
  out+="- Binds: **\`:name\` only** — no positional \`?\`; optional filters via \`COALESCE(:param, '')\` in SQL, not Java string building"$'\n'
  out+="- Infra: \`InjectSql\` + \`SqlInjectBeanPostProcessor\` in \`core\`"$'\n'
  out+="- IT cleanup: Spring \`@Sql\` scripts (not repository API)"$'\n'
  out+=$'\n'"## Tests (DEC-009)"$'\n'
  out+="- \`mvn test\` — unit + Modulith (no Docker)"$'\n'
  out+="- \`mvn verify -Pintegration\` — Testcontainers; run from **WSL** on Windows"$'\n'
  out+="- \`mvn verify -Pquality\` — full test-split benchmarks (M8+)"$'\n'
  out+="- \`mvn verify -Pprompt-lab\` — prompt-lab eval + MCP tools IT"$'\n'
  out+="- \`mvn verify -Pprompt-lab-quality\` — retrieval + prompt-lab test-split gates"$'\n'
  out+=$'\n'"## User guides"$'\n'
  out+="End-user how-to: [docs/guides/README.md](../../docs/guides/README.md)"$'\n'

  stage "$PLANS/00-index.md" "$out"
}

# ---- run -------------------------------------------------------------------

gen_active_context
gen_progress
gen_decisions
gen_product_context
gen_plans_index

flush