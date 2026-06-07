# Playbook: Red build triage

**Tool:** OpenCode  
**Primary:** **Plan** (triage only; Tab to **Build** for fix)  
**Subagents:** `@explore` (map) + bash verify + `@general` (diagnose)  

**When:** CI red, local `mvn test` fails, or standup needs facts in 10 minutes.

---

## Triggers

- Main branch broken
- PR check `unit-tests` failed
- Flaky test suspected (still start with reproduce)

---

## Orchestration prompt

```text
Red build triage on <ABSOLUTE_REPO_PATH>/<service-module>:

Parallel:
1) @explore read-only: map package(s) related to failure; list tests for affected area.
2) bash: <mvn test | mvn verify> — failures only with assertion text.
3) Read log: <paste log excerpt OR path to saved log file>
   Diagnose top failure — root cause + minimal fix proposal (no implementation).

Merge for lead review:
- Architecture (3 bullets)
- Reproduction command + result
- Root cause + minimal fix proposal
- Recommended single next action
```

Template: [templates/triage-session.md](../templates/triage-session.md)

---

## Custom command

```bash
/triage
```

After installing `.opencode/commands/triage.md` from template.

---

## Lead merge template

```markdown
## Triage — <service> — <date>
**Red:** <test or check name>
**Cause:** <one line>
**Next:** <one action, one owner>
**Out of scope:** <what we are NOT doing today>
```

---

## Acceptance criteria

- [ ] Reproduction command documented  
- [ ] Root cause references class + method  
- [ ] No drive-by refactors in triage phase  

---

## Stop conditions

- Diagnosis wants multi-module rewrite → narrow to one module  
- No log and no local reproduce → stop; get artifact first  

---

## Guide reference

[Section 1: Parallel triage](../README.md#section-1-parallel-triage-red-build)
