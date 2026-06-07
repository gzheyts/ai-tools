# Template: Incident hotfix

Single root cause, minimal diff, fast verify. No refactoring.

---

## Customize

| Marker | Replace with |
|--------|--------------|
| `[REPO_PATH]` | Absolute repo path |
| `[SYMPTOM]` | User-visible or metric symptom |
| `[LOG]` | CI log path or pasted excerpt |
| `[FAST_TEST]` | `mvn test -Dtest=FooTest` or smoke command |
| `[MAX_FILES]` | Usually `1` |

---

## Phase 1 — Diagnose (Plan primary)

```text
Plan primary — incident hotfix triage only (no edits):

Repo: [REPO_PATH]
Symptom: [SYMPTOM]
Log: [LOG]

1. Parse failing check / test from log
2. Root cause with evidence (log line + code line)
3. Minimal patch description (≤30 lines typical)
4. Rollback risk (one sentence)

Do NOT implement until I approve.
```

---

## Phase 2 — Fix (Build primary)

```text
Build primary — apply approved patch only.

Constraints:
- Touch at most [MAX_FILES] file(s)
- No refactoring, no dependency upgrades, no formatting sweeps

Run: [FAST_TEST] then full mvn test if fast test passes.
Return: cause recap, diff summary, test output.
```

---

## Expected artifacts

- One root cause, one primary file changed
- Test output with `Failures: 0`

## Reject if

- Framework upgrade proposed
- Multiple unrelated files changed
- Assertion deleted to green tests

---

## Interrupt prompt (doom loop)

```text
Stop. Summarize what you tried, what failed, and what you need from me.
Do not run more commands until I reply.
```

See [Section 0d: Token efficiency](../README.md#section-0d-token-efficiency-and-doom-loop-prevention).
