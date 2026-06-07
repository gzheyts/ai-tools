# AI Assistant Onboarding Guide

Welcome to the team! This guide will help you set up and use AI coding
assistants effectively with our projects.

## 1. Setup (Day 1)

### Install Your AI Assistant

- [ ] Install [Cursor](https://cursor.sh/) / OpenCode / Yandex SourceCraft
- [ ] Verify the CLI works: `cursor --version` (or equivalent)
- [ ] Configure the model (recommended: Claude Sonnet for daily work)

### Understand the Project's AI Context

- [ ] Read `AGENTS.md` in the project root — this is the AI's "manual"
- [ ] Review `context-map.md` — learn which files to open per task type
- [ ] Check `.cursorignore` — understand what's excluded from AI context

### Explore Available Skills and Commands

Skills (loaded automatically when relevant):

| Skill | Purpose |
|-------|---------|
| `code-review` | Reviews code for quality, security, and conventions |
| `generate-tests` | Generates JUnit 5 tests with BDD structure |
| `db-migration` | Generates Liquibase XML changesets |
| `schema-review` | Reviews database schema for issues |
| `ci-fix` | Diagnoses GitLab CI pipeline failures |

Commands (invoked manually with `/command`):

| Command | Purpose | Example |
|---------|---------|---------|
| `/review` | Review code in current file | Select code, run `/review` |
| `/test` | Generate tests for a class | Open class, run `/test` |
| `/migration` | Generate a Liquibase migration | `/migration add email column to persons` |
| `/endpoint` | Generate a REST endpoint | `/endpoint Order` |
| `/ci-fix` | Debug CI failure | Paste error, run `/ci-fix` |
| `/k8s-debug` | Debug K8s issue | Paste logs, run `/k8s-debug` |

## 2. First-Week AI Challenge

| Day | Task | How |
|-----|------|-----|
| 1 | Set up assistant, read AGENTS.md | Follow section 1 above |
| 2 | Generate a CRUD endpoint | Use `/endpoint` command |
| 3 | Generate a database migration | Use `/migration` command |
| 4 | Generate and run tests | Use `/test` command, then `./mvnw test` |
| 5 | Review your own code | Use `/review` command on Day 2-4 code |

## 3. Team Conventions for AI Usage

### Do

- Start new chats every 10-15 messages (prevents context rot)
- Close irrelevant file tabs before starting a task
- Review all AI-generated code before committing
- Use skills and commands instead of typing instructions manually
- Report AI quality issues so we can improve skills

### Don't

- Don't paste secrets, passwords, or production credentials into chat
- Don't blindly accept AI-generated code without review
- Don't use AI for files in `.cursorignore`
- Don't commit AI-generated code that doesn't compile or pass tests
- Don't modify `AGENTS.md` without team review (create a PR)

### Proposing Changes to AI Config

To improve a skill, command, or AGENTS.md:

1. Create a branch: `improve/skill-code-review`
2. Make your changes
3. Test on at least 3 representative inputs
4. Create a PR with the prompt-change template
5. Get approval from a team member

## 4. Troubleshooting

| Issue | Fix |
|-------|-----|
| AI doesn't follow AGENTS.md conventions | Start a new chat (context rot) |
| AI hallucinates non-existent classes | Close unrelated tabs, provide specific context |
| AI generates incorrect SQL | Use MCP Postgres to let it inspect the real schema |
| Generated tests don't actually test anything | Review assertions, check for `assertTrue(true)` |
| AI is slow to respond | Check if you have too many files open |

## 5. Resources

- Notes modules: `notes/modules/`
- Sample project: `notes/solutions/sample-project/`
- Templates: `notes/templates/`
