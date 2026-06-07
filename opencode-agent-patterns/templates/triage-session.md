# Template: Parallel triage session

Copy into OpenCode chat (Plan primary) or save as `.opencode/commands/triage.md`.

---

## Customize

| Marker | Replace with |
|--------|--------------|
| `[REPO_PATH]` | Absolute path to Java module (e.g. `<project_root>/templates/sample-project`) |
| `[PACKAGE]` | Root package (e.g. `com.example.demo`) |
| `[TEST_CMD]` | `mvn test` or `mvn verify` |
| `[CI_LOG]` | Path to log file or “paste excerpt below” |

---

## Prompt

```text
Plan primary — orchestrate parallel triage on [REPO_PATH]:

1) @explore read-only: map package [PACKAGE] — production classes, test classes per class, known risk areas.
2) Run [TEST_CMD] (bash); report failures only with assertion text and exit code.
3) Read [CI_LOG]; diagnose top failure — root cause with evidence (log line + source line), minimal fix proposal (no edits).

Merge for lead review:
- Architecture (3 bullets)
- Reproduction: command + result
- Root cause + minimal fix proposal
- One recommended next action for my approval

Constraints:
- Triage only — no file edits
- No drive-by refactors
```

---

## Expected artifacts (accept)

- Package map with class names and test coverage notes
- Exact failing test method names
- Root cause references **class + method + line logic**
- Single proposed next action

## Reject if

- “Tests should pass” without command output
- Multi-module rewrite proposed for one red test
- Unexpected file edit during Plan phase

---

## After approval

Tab to **Build**:

```text
Build primary — apply approved minimal fix only.
Run [TEST_CMD]. Paste Tests run / Failures / Errors.
Return: files changed, test output.
```
