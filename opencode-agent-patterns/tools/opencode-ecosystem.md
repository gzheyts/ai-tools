# OpenCode ecosystem — skills, agents, MCP (Java focus)

Curated community resources for Java developers. Install what you need; audit third-party skills like any dependency.

---

## Official documentation

| Resource | URL |
|----------|-----|
| OpenCode docs | https://opencode.ai/docs/ |
| Agents | https://opencode.ai/docs/agents/ |
| Skills | https://opencode.ai/docs/skills/ |
| Commands | https://opencode.ai/docs/commands/ |
| Rules / AGENTS.md | https://opencode.ai/docs/rules/ |
| AGENTS.md standard | https://agents.md/ |

---

## Community skill repositories

| Repository | Contents | Java relevance |
|------------|----------|----------------|
| [vekzz-dev/opencode-skills](https://github.com/vekzz-dev/opencode-skills) | JUnit 5, Spring Boot, git-commit, changelog | **High** |
| [shcherbi/open-code-ai-java](https://github.com/shcherbi/open-code-ai-java) | 18 skills: review, concurrency, security, Maven audit | **Very high** |
| [weisser-dev/awesome-opencode](https://github.com/weisser-dev/awesome-opencode) | 108 agents, 15 skills, 18 MCP servers, interactive installer | General + Java |
| [farmage/opencode-skills](https://github.com/farmage/opencode-skills) | 66 skills, 9 workflow commands, Java domain | **High** |
| [github/awesome-copilot](https://github.com/github/awesome-copilot) | Copilot skills; many work in OpenCode (same SKILL.md format) | Java MCP generator, etc. |

### Quick install — Java testing skills

```bash
git clone https://github.com/vekzz-dev/opencode-skills.git /tmp/opencode-skills
mkdir -p ~/.config/opencode/skills
cp -r /tmp/opencode-skills/java-junit \
      /tmp/opencode-skills/java-springboot \
      /tmp/opencode-skills/spring-boot-testing \
      ~/.config/opencode/skills/
```

### Quick install — Java review / security pack

```bash
git clone https://github.com/shcherbi/open-code-ai-java.git /tmp/open-code-ai-java
cd /tmp/open-code-ai-java
# See repo scripts/setup-project.sh or link-skills.sh for symlinks
```

### Interactive bundle (agents + skills + MCP)

```bash
npx @weisser-dev/awesome-opencode
```

---

## Skills worth installing (Java teams)

| Skill | Source | Use when |
|-------|--------|----------|
| `java-junit` | vekzz-dev | Adding/fixing unit tests |
| `spring-boot-testing` | vekzz-dev | `@WebMvcTest`, Testcontainers, MockMvc |
| `java-springboot` | vekzz-dev | REST, DI, production patterns |
| `java-code-review` | shcherbi | PR review, Effective Java checks |
| `concurrency-review` | shcherbi | `ExecutorService`, virtual threads |
| `maven-dependency-audit` | shcherbi | CVE / outdated deps |
| `security-audit` | shcherbi | OWASP-style pass |
| `test-quality` | shcherbi | Coverage gaps, flaky tests |
| `git-commit` | vekzz-dev / shcherbi | Conventional commits |

Invoke: `Use the java-code-review skill on PaymentService.java`

---

## MCP servers (recommended)

Configure in `opencode.json` under `mcp`. Audit servers before production use.

| MCP | Benefit for Java |
|-----|------------------|
| **context7** | Up-to-date Spring, JUnit, Hibernate docs — add to `AGENTS.md`: “Use context7 for library API questions” |
| **GitHub** | Issues, PRs, `gh run` logs in agent context |
| **Atlassian** | Jira/Confluence (see farmage workflow commands) |
| **Composio** | SaaS integrations (Linear, Slack, etc.) via unified CLI |

Example `AGENTS.md` line:

```markdown
When Spring Boot or JUnit API is unclear, use context7 MCP for current documentation.
```

---

## Multi-agent extensions

| Project | What it adds |
|---------|--------------|
| [oh-my-opencode](https://opencodedocs.com/code-yeongyu/oh-my-opencode/advanced/background-tasks/) | Background parallel tasks, concurrency limits |
| [OpenCode-Book Lab 5](https://www.opencodebook.xyz/en/chapter_17_hands-on_labs/17.5_lab5_simplified_multi-agent_orchestration_system) | Simplified orchestration patterns |
| [Multi-agent workflow article](https://codecraftersden.com/opencode-multi-agent-workflow/) | `WORKFLOW_STATE.md`, role separation |

Start with built-in Explore / General / Scout before adding extensions.

---

## Resources

| Resource | Topic |
|----------|-------|
| [README](../README.md) | Overview |
| [README.md — Section 0b](../README.md#section-0b-how-agentic-workflow-executes-opencode-internals) | Execution internals |
| [README.md — Section 0c](../README.md#section-0c-commands-skills-and-agentsmd-opencode) | Commands + SKILL.md |
| [README.md — Section 0d](../README.md#section-0d-token-efficiency-and-doom-loop-prevention) | Doom loops, `steps` limit |
| [templates/](../templates/) | Copy-paste workflow templates |
| [Token optimization (TrueFoundry)](https://www.truefoundry.com/blog/opencode-token-usage-how-it-works-and-how-to-optimize-it) | Cost control |

---

## Demo project

[../templates/sample-project/](../../templates/sample-project/) — intentional `PersonServiceImpl` bug, `PersonServiceImpl` refactor target, CI log fixture at `ci-logs/unit-tests-failure.log`.
