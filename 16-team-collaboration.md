# Section 17: Team Collaboration

Individual AI assistant setups get you started. Team-wide AI practices
multiply the benefit. This section covers how to share AI configurations,
maintain prompt quality across a team, onboard new developers with AI
context, and review AI-related changes in pull requests.

| Section | Topic |
|---------|-------|
| [1. Sharing AGENTS.md](#1-sharing-agentsmd-across-a-team) | Repository strategy, multi-repo sharing |
| [2. Skills as a Team Library](#2-skills-as-a-team-library) | Organization, naming, review process |
| [3. Onboarding New Developers](#3-onboarding-new-developers-with-ai-context) | Checklist, first-week challenge |
| [4. Prompt Review in PRs](#4-prompt-review-in-pull-requests) | When to review, PR template |
| [5. Measuring Effectiveness](#5-measuring-team-ai-effectiveness) | Metrics, retrospective questions |

---

## 1. Sharing AGENTS.md Across a Team

### Strategy: AGENTS.md Lives in the Repository

The simplest and most effective approach: commit `AGENTS.md` to the project
repository root. Every developer who clones the repo gets the same AI context.

```
my-service/
├── AGENTS.md              ← committed, team-maintained
├── src/
├── pom.xml
└── ...
```

**Benefits:**
- Versioned with the code
- Changes go through code review
- Every team member sees the same conventions
- Works with all three assistants

**Maintenance:**
- Assign an owner (or rotate ownership per sprint)
- Review AGENTS.md during sprint retrospectives
- Update when stack versions, patterns, or conventions change

### Strategy: Shared AGENTS.md Across Multiple Repositories

When several microservices share the same stack and conventions:

**Option A: Git Submodule**

```bash
git submodule add https://gitlab.com/team/ai-prompts.git .ai-prompts
```

Then in `AGENTS.md`:
```markdown
<!-- Include shared conventions from .ai-prompts/AGENTS-shared.md -->
## Project-Specific Context
...
```

Pros: version pinning, explicit updates.
Cons: submodule complexity, merge conflicts.

**Option B: Copy + Customize**

Maintain a "golden" AGENTS.md in a central repo. Each project copies it and
adds project-specific sections. Use a script to diff against the golden
version periodically.

```bash
diff <(curl -s https://gitlab.com/team/ai-prompts/raw/main/AGENTS-base.md) AGENTS.md
```

Pros: full control per project.
Cons: drift over time without discipline.

**Option C: Symlinks via GNU Stow**

For developers who work on multiple repos on one machine:

```bash
cd ~/ai-prompts
stow -t ~/projects/service-a agents
stow -t ~/projects/service-b agents
```

Pros: instant updates, no Git overhead.
Cons: local-only, doesn't help CI or other developers.

## 2. Skills as a Team Library

### Organizing a Shared Skill Library

```
ai-prompts/
├── skills/
│   ├── code-review/
│   │   └── SKILL.md           # v1.2.0
│   ├── generate-tests/
│   │   └── SKILL.md           # v1.1.0
│   ├── db-migration/
│   │   └── SKILL.md           # v2.0.0
│   ├── security-review/
│   │   └── SKILL.md           # v1.0.0
│   └── endpoint-generator/
│       └── SKILL.md           # v1.3.0
├── commands/
│   ├── cursor/
│   └── opencode/
├── CHANGELOG.md
└── README.md
```

### Skill Naming Conventions

| Convention           | Example                         | Rule                          |
|----------------------|---------------------------------|-------------------------------|
| Directory name       | `code-review/`                  | kebab-case, noun or verb-noun |
| Skill title          | `Code Review`                   | Title Case in frontmatter     |
| Version              | `v1.2.0`                        | Semantic versioning           |
| Description          | `Reviews Java code for...`      | One sentence, starts with verb|

### Skill Review Process

Treat skills like code — they go through review:

1. **Create a branch**: `feature/update-code-review-skill`
2. **Make changes**: Update the SKILL.md
3. **Test**: Run the skill on 3 representative inputs
4. **Document**: Update CHANGELOG.md with what changed and why
5. **Review**: Another team member reviews the prompt changes
6. **Merge**: After approval, merge and bump version

### What to Review in a Skill PR

| Check                                    | Why                                       |
|------------------------------------------|-------------------------------------------|
| Instructions are specific and actionable | Vague instructions produce inconsistent output |
| Few-shot examples match current patterns | Outdated examples teach wrong patterns    |
| Output format is clear and parseable     | Downstream tools may depend on the format |
| No contradictions with AGENTS.md         | Conflicts confuse the AI                  |
| Version bumped appropriately             | Consumers need to know about changes      |
| Tested on representative inputs          | Catch regressions before merge            |

## 3. Onboarding New Developers with AI Context

Use [templates/team-onboarding.md](templates/team-onboarding.md) as the
copy-paste onboarding doc for new hires. It includes the Day 1 checklist,
first-week challenge, do/don't conventions, troubleshooting table, and links
to this repo's guides.

### Team-specific onboarding practices

Beyond the template checklist:

1. **Assign an onboarding buddy** who has shipped at least one AI-assisted PR
   using your project's skills and commands.
2. **Walk through `context-map.md`** in the project repo — show which files
   to open for a migration vs a bug fix ([Section 6: Context](06-context.md)).
3. **First PR expectation:** new developers should use `/review` on their own
   diff before requesting human review (Section 4 below).
4. **Subagent intro (optional):** after the first week, point them to
   [Section 12: Agents & Subagents](12-agents-subagents.md) for multi-step
   workflows — not on Day 1.

Customize the template per project: update the skills/commands table to match
what you actually ship in `.cursor/commands/` and `skills/`.

## 4. Prompt Review in Pull Requests

### When AI Config Changes Need Review

Changes to these files should go through code review:

| File                    | Review Focus                                    |
|-------------------------|-------------------------------------------------|
| `AGENTS.md`             | Accuracy, completeness, no contradictions        |
| `skills/*/SKILL.md`     | Instructions clarity, few-shot quality, version  |
| `.cursor/commands/*.md`  | Argument handling, output format, safety         |
| `.opencode/commands/*.md`| Same as above                                   |
| `.cursor/mcp.json`       | Security (no hardcoded credentials)             |

### PR Template for Prompt Changes

```markdown
## Prompt Change

**What changed:** [Describe the change to AGENTS.md / skill / command]

**Why:** [What problem does this solve? What improvement is expected?]

**Testing:**
- [ ] Tested with input A: [describe result]
- [ ] Tested with input B: [describe result]
- [ ] Tested with edge case: [describe result]

**Backward compatibility:**
- [ ] No downstream skills/commands depend on changed behavior
- [ ] OR: downstream consumers updated

**Version bump:** [e.g., v1.1.0 → v1.2.0]
```

### Review Checklist for AI-Generated Code in PRs

When reviewing code that was generated with AI assistance:

1. **Does it follow AGENTS.md conventions?** — The AI sometimes ignores rules
2. **Are there hallucinated imports?** — AI may reference non-existent classes
3. **Is the test coverage real?** — AI-generated tests sometimes don't actually test behavior
4. **Are edge cases handled?** — AI often focuses on the happy path
5. **Is the error handling correct?** — Check for generic catch blocks
6. **Are there security issues?** — SQL injection, missing validation, exposed secrets
7. **Does the naming follow conventions?** — Check method/variable/class names

## 5. Measuring Team AI Effectiveness

### Metrics to Track

| Metric                              | How to Measure                           | Target               |
|-------------------------------------|------------------------------------------|-----------------------|
| AI-assisted PR ratio                | PRs tagged with AI-assisted / total PRs  | Track trend, not target|
| Time from story to PR               | JIRA timestamps                          | Compare before/after  |
| Code review turnaround              | Time from PR creation to approval        | Faster with AI review |
| Prompt library usage                | Git log on skills/commands               | Growing adoption      |
| AGENTS.md update frequency          | Git log on AGENTS.md                     | At least monthly      |

### Retrospective Questions

Add these to your sprint retrospective:

1. Did the AI save time this sprint? On which tasks?
2. Did the AI produce incorrect code that took extra time to fix?
3. Are there new patterns we should add to AGENTS.md?
4. Are any skills producing inconsistent output?
5. Should we invest time in a new skill or command?

## Quick-Reference Cheat Sheet

| Practice                    | Implementation                                    |
|-----------------------------|---------------------------------------------------|
| Share AGENTS.md             | Commit to repo root, review changes               |
| Share skills                | Centralized library repo with versioning           |
| Onboard new devs            | Checklist + first-week AI challenge                |
| Review prompt changes       | PR template + testing checklist                    |
| Review AI-generated code    | 7-point checklist focusing on convention adherence |
| Measure effectiveness       | Track AI-assisted PR ratio and time savings        |

---

## Next Section

Proceed to [Section 17: Security & Privacy](17-security-and-privacy.md)
to learn how to use AI assistants safely with sensitive codebases.

### Further Reading

- [Section 9: AGENTS.md](09-agents-md.md) for writing project context
- [Section 10: Skills](10-skills.md) for building reusable skills
- [Section 3: Prompt Management](03-prompting.md#multi-project-prompt-management) for multi-project
  prompt storage
