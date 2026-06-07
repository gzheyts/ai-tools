# Section 14: Team Collaboration

Individual AI assistant setups get you started. Team-wide AI practices
multiply the benefit. This section covers how to share AI configurations,
maintain prompt quality across a team, onboard new developers with AI
context, and review AI-related changes in pull requests.

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
│   ├── opencode/
│   └── sourcecraft/
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

### Onboarding Checklist

When a new developer joins the team:

1. **Install the AI assistant** (Section 1)
   - [ ] Cursor / OpenCode / SourceCraft installed
   - [ ] CLI configured and working
   - [ ] Model settings configured (which model, thinking mode)

2. **Understand the AI context** (Sections 1-2)
   - [ ] Read AGENTS.md and understand each section
   - [ ] Know which skills are available and what they do
   - [ ] Know which commands are available and when to use them

3. **Practice with a starter task** (Sections 5-7)
   - [ ] Use `/endpoint` to generate a simple CRUD endpoint
   - [ ] Use `/migration` to create a database migration
   - [ ] Use `/test` to generate tests for the endpoint
   - [ ] Use `/review` to review the generated code

4. **Learn the team workflow** (this section)
   - [ ] How to propose changes to AGENTS.md
   - [ ] How to submit a new skill or improve an existing one
   - [ ] When to use subagents vs single chat
   - [ ] How to handle AI-generated code in PRs

### First-Week AI Challenge

Give new developers a structured first-week exercise:

| Day | Task                                          | Sections to Reference |
|-----|-----------------------------------------------|---------------------|
| 1   | Set up the assistant, read AGENTS.md          | 0, 5                |
| 2   | Generate a simple endpoint using commands     | 7, 9                |
| 3   | Generate and debug a database migration       | 7, 10               |
| 4   | Use a subagent workflow for a small feature   | 8                   |
| 5   | Review a PR with the code-review skill        | 6                   |

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

Proceed to [Section 15: Security & Privacy](15-security-and-privacy.md)
to learn how to use AI assistants safely with sensitive codebases.

### Further Reading

- [Section 8: AGENTS.md](08-agents-md.md) for writing project context
- [Section 9: Skills](09-skills.md) for building reusable skills
- [Section 3: Prompt Management](03-prompting.md#multi-project-prompt-management) for multi-project
  prompt storage
