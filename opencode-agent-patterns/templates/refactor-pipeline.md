# Template: Safe refactor pipeline

Plan → approve → implement → verify. For duplication removal, extract method, internal API cleanup.

---

## Customize

| Marker | Replace with |
|--------|--------------|
| `[REPO_PATH]` | Absolute repo path |
| `[TARGET_CLASS]` | e.g. `OrderService.java` |
| `[FOCUSED_TEST]` | e.g. `OrderServiceTest` |
| `[ALLOWED_FILES]` | e.g. `OrderService.java` only |
| `[RULES]` | Discount rules, invariants, public API constraints |

---

## Phase 1 — Map + plan (Plan primary)

```text
Plan primary — refactor pipeline phases 1–2 only on [REPO_PATH]:

Phase 1 — @explore read-only:
- Document duplication in [TARGET_CLASS]
- List tests that guard behavior
- Note [RULES]

Phase 2 — @general plan only (no edits):
- Propose private method extractions with signatures and call sites
- Confirm [FOCUSED_TEST] expectations unchanged

Stop and return plan for my approval.
```

---

## Phase 2 — Implement (Build primary, after approval)

Paste approved plan, then:

```text
Build primary — implement the approved plan only.

Files allowed: [ALLOWED_FILES]
Rules: [RULES]
- No public method signature changes
- No new dependencies

Then run:
  mvn test -Dtest=[FOCUSED_TEST]
  mvn test

Return: files changed, test output, 3-line before/after summary.
```

---

## Expected artifacts

- Plan lists concrete method names (`sumLineTotals`, `applyDiscount`, …)
- Diff limited to allowed files
- Focused test green, then full suite green

## Reject if

- Public API changed without ticket
- Test deleted or `@Disabled` added
- Diff > ~200 lines for “small” refactor

---

## Optional: custom command

Save phase 1 as `.opencode/commands/refactor-plan.md` with `agent: plan`.
