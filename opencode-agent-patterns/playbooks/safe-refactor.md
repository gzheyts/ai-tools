# Playbook: Safe refactor (Java)

**Tool:** OpenCode  
**Primary:** **Plan** (map + plan) → **Build** (implement + verify)  
**Subagents:** `@explore`, `@general`  

**When:** Duplication, extract method — behavior must stay identical.

---

## Constraints (always in prompt)

```text
- Do not change public method signatures
- Run mvn test -Dtest=<FocusedTest> then mvn test
- No new dependencies without explicit approval
- Plan-only until lead approves
```

---

## Phase 1 — Map + plan

```text
Safe refactor pipeline — plan phases only:

1) @explore read-only: <target classes> — duplication, coupling, tests that guard behavior.
2) @general plan only: extraction plan with method signatures and call sites.

Repo: <ABSOLUTE_PATH>
Stop before any edits.
```

Template: [templates/refactor-pipeline.md](../templates/refactor-pipeline.md)

---

## Phase 2 — Implement (after approval)

```text
Build primary — implement approved plan exactly:

<paste approved plan>

Files allowed: <list>
Run mvn test -Dtest=<FocusedTest> && mvn test
Return: diff summary + test output.
```

Sequential General pattern: [Section 2: Refactor pipeline](../README.md#section-2-refactor-pipeline-sequential-agents).

Optional skill: `java-code-review` on diff before merge.

---

## Custom command

`/refactor-plan` — save phase 1 as `.opencode/commands/refactor-plan.md`

---

## Acceptance criteria

- [ ] Focused test class green  
- [ ] Full suite green  
- [ ] Public API unchanged (`@explore` or `git diff` confirm)  

---

## Stop conditions

- Diff > ~200 lines for “small” refactor → split PR  
- Test deleted or `@Disabled` added → reject  

---

## Demo practice

[Section 2: Refactor pipeline](../README.md#section-2-refactor-pipeline-sequential-agents) + `OrderService`
