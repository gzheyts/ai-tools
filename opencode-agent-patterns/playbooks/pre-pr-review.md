# Playbook: Pre-PR review (agent-assisted)

**Tool:** OpenCode  
**Primary:** **Plan** (scope-only audit) or **Build** (verify)  
**Subagents:** `@explore` (read-only scope)  

**When:** Before approving a PR or after agent edits on your branch.

---

## Triggers

- Agent just implemented a feature/fix  
- Large diff from junior + agent pair  
- You need confidence tests actually ran  

---

## Orchestration prompt

```text
Pre-PR review on <ABSOLUTE_REPO_PATH>:

1) bash: mvn verify (or project standard). Paste Tests run / Failures / Errors.
2) @explore read-only: list files changed under <package> — flag anything outside <expected packages> (pom.xml, generated/, security config).
3) Summarize: test status, scope OK yes/no, risks for reviewer.

Do not modify code.
```

Template: [templates/pr-review.md](../templates/pr-review.md)

---

## Custom command

`/pre-pr` — from `.opencode/commands/pre-pr.md`

Optional: `Use java-code-review skill` for Effective Java / concurrency checks.

---

## Lead checklist

- [ ] Verify output is **pasted**, not asserted  
- [ ] No `pom.xml` unless expected  
- [ ] No generated/target files in diff  
- [ ] Test names match behavior change  

---

## Acceptance criteria

- `mvn verify` (or team equivalent) green  
- Scope matches ticket  

---

## Stop conditions

- Verify skipped → run bash yourself before approving PR  
- Unexpected security/config files → human review mandatory  

---

## Guide reference

[Section 3: PR-ready workflow](../README.md#section-3-pr-ready-workflow-ci-log-verify)
