# ai-tools

My personal notes on using **AI coding assistants** as a Java developer.

---

## What this covers

- **How the runtime actually works** — agent loop, tools, context limits — so failures feel debuggable instead of mystical.
- **Project-level wiring** — `AGENTS.md`, `SKILL.md`, slash commands, subagents — so the assistant shows up *already aligned* with my architecture and standards.
- **Java / Spring / DevOps reality** — migrations, CI, Helm, K8s.

---

## Stack I optimize for

| Layer         | Technology |
|---------------|------------|
| Language      | Java 21+ (records, sealed classes, virtual threads) |
| Framework     | Spring Boot 3.5+, Spring Data JDBC |
| Build         | Maven 3.9+ |
| CI/CD         | GitLab CI |
| Orchestration | Kubernetes + Helm |
| Databases     | PostgreSQL, Oracle |
| Assistants    | Cursor (+ CLI), OpenCode |

---

## Templates

Ready-to-use files in [`templates/`](templates/):

| File | Description |
|------|-------------|
| `AGENTS.md` | Project context template |
| `SKILL.md` | Skill template |
| `context-map.md` | Context map template |
| `team-onboarding.md` | Team onboarding guide |
| `command-cursor.md` | Cursor slash command template |
| `command-opencode.md` | OpenCode command template |
| `mcp-config-cursor.json` | MCP config for Cursor |
| `mcp-config-opencode.json` | MCP config for OpenCode |
| `liquibase-changelog.xml` | Liquibase changelog template |
| `sample-project/` | Reference Maven project with skills, commands, and AGENTS.md — demo target for [OpenCode workflows](opencode-agent-patterns/README.md) |

`context-map.md` and `team-onboarding.md` are canonical templates; `sample-project/` holds project-specific overrides (paths, skill lists) where needed.

---

## OpenCode agent patterns

[`opencode-agent-patterns/`](opencode-agent-patterns/) — hands-on **multi-agent orchestration** with [OpenCode](https://opencode.ai): parallel triage, refactor pipelines, PR-ready CI fixes, doom-loop prevention.

Start at [opencode-agent-patterns/README.md](opencode-agent-patterns/README.md) — concepts, workflow walkthroughs (sections 0–5), prompts, and cookbook in one file.

| Resource | Description |
|----------|-------------|
| [README.md](opencode-agent-patterns/README.md) | Full guide — primary/subagent model, execution internals, commands, skills, Java examples |
| [templates/](opencode-agent-patterns/templates/) | Copy-paste `/triage`, `/hotfix`, refactor, PR-review commands |
| [playbooks/](opencode-agent-patterns/playbooks/) | Red-build triage, safe refactor, pre-PR review, incident hotfix |
| [tools/](opencode-agent-patterns/tools/) | OpenCode cheat sheet, ecosystem links, example `opencode.json` agents |

Practice on [`templates/sample-project/`](templates/sample-project/) — includes an intentional failing test for triage exercises.

Complements [11-agents-subagents.md](11-agents-subagents.md) (assistant-agnostic concepts) with OpenCode-specific workflows.

---

## Sections

### Foundations
| # | File | Topic |
|---|------|-------|
| 01 | [01-setup.md](01-setup.md) | Environment setup |
| 02 | [02-how-ai-assistants-work.md](02-how-ai-assistants-work.md) | How AI assistants work |

### Prompting
| # | File | Topic |
|---|------|-------|
| 03 | [03-prompting.md](03-prompting.md) | Prompting fundamentals & management |
| 04 | [04-prompt-techniques.md](04-prompt-techniques.md) | Prompt techniques & best practices |
| 05 | [05-llm-models.md](05-llm-models.md) | LLM thinking & model selection |
| 06 | [06-context.md](06-context.md) | Context strategies & engineering |
| 07 | [07-prompt-optimization.md](07-prompt-optimization.md) | Prompt optimization |

### Configuration
| # | File | Topic |
|---|------|-------|
| 08 | [08-agents-md.md](08-agents-md.md) | AGENTS.md |
| 09 | [09-skills.md](09-skills.md) | Skills |
| 10 | [10-custom-commands.md](10-custom-commands.md) | Custom commands |
| 11 | [11-agents-subagents.md](11-agents-subagents.md) | Agents & subagents — see also [opencode-agent-patterns/README.md](opencode-agent-patterns/README.md) |
| 12 | [12-agent-sessions.md](12-agent-sessions.md) | Agent sessions — lifecycle, child sessions, compaction, cross-session handoff |
| 13 | [13-mcp-servers.md](13-mcp-servers.md) | MCP architecture, tool resolution, configuration |

### Java & DevOps
| # | File | Topic |
|---|------|-------|
| 14 | [14-java-production-stack.md](14-java-production-stack.md) | Java / Spring Boot, database & DevOps |

### Advanced
| # | File | Topic |
|---|------|-------|
| 15 | [15-team-collaboration.md](15-team-collaboration.md) | Team collaboration |
| 16 | [16-security-and-privacy.md](16-security-and-privacy.md) | Security & privacy |
