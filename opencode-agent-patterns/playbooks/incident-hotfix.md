# Playbook: Incident hotfix

**Tool:** OpenCode  
**Primary:** **Plan** (diagnose) → **Build** (minimal fix)  
**Subagents:** Plan + log → approve → `@general` — no parallel refactor  

**When:** Production issue; **minimal** diff fast.

---

## Constraints (mandatory)

```text
- Minimal diff — single root cause
- Touch at most 1–2 files unless incident requires more
- No refactoring, no dependency upgrades, no formatting sweeps
- mvn test (or smoke test command) required in summary
```

---

## Orchestration prompt

**Phase 1 — Plan primary:**

```text
Incident hotfix — diagnose only:

Repo: <ABSOLUTE_PATH>
Symptom: <user-visible or metric>
Log: <path or pasted excerpt>

1. Root cause with evidence (log line + code line)
2. Minimal patch description
3. Rollback risk (one sentence)

Do NOT implement until I approve.
```

**Phase 2 — Build primary:**

```text
Build primary — apply approved patch only.
Run: <fast test command> then mvn test if applicable.
Paste test output.
```

Template: [templates/hotfix.md](../templates/hotfix.md)

---

## Custom command

`/hotfix <log-path>` — from `.opencode/commands/hotfix.md`

---

## Acceptance criteria

- [ ] One root cause documented  
- [ ] Patch < ~30 lines typical  
- [ ] Tests green  

---

## Stop conditions

- Agent proposes framework upgrade → reject  
- Multiple unrelated files → split; fix production path only first  
- Doom loop after 3 failures → [Section 0d: Token efficiency](../README.md#section-0d-token-efficiency-and-doom-loop-prevention)

---

## Demo walkthrough

[Section 3: PR-ready workflow](../README.md#section-3-pr-ready-workflow-ci-log-verify) with `Calculator` + CI log
