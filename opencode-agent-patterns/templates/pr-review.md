# Template: Pre-PR review (agent-assisted)

Read-only scope audit + verify gate. Use before merging agent-generated work.

---

## Customize

| Marker | Replace with |
|--------|--------------|
| `[REPO_PATH]` | Absolute repo path |
| `[PACKAGE]` | Expected package scope (e.g. `com.example.demo`) |
| `[VERIFY_CMD]` | `mvn verify` or team standard |

---

## Prompt (Plan or Build)

```text
Pre-PR review on [REPO_PATH] — do not modify code.

1) Run [VERIFY_CMD] (bash). Paste Tests run / Failures / Errors.
2) @explore read-only: list files changed under [PACKAGE] in this session/branch; flag anything outside expected scope (pom.xml, generated/, security config).
3) Summarize: test status, scope OK yes/no, risks for human reviewer.

Return format:
- Verify: (paste output)
- Scope: (file list + surprises)
- PR-ready: yes/no + reason
```

---

## Lead checklist

- [ ] Verify output is **pasted**, not asserted
- [ ] No `pom.xml` unless expected
- [ ] No `target/` or generated artifacts in diff
- [ ] Test names match behavior change

## Reject if

- Verify skipped
- Scope creep into unrelated modules
- Security-sensitive paths touched without review

---

## Custom command shortcut

`.opencode/commands/pre-pr.md`:

```markdown
---
description: Pre-PR verify + scope audit
agent: plan
---

Pre-PR review on [REPO_PATH] ...
```

Run: `/pre-pr`
