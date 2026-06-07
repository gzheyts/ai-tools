# OpenCode quick reference

Single-page lookup for agents, invocation, commands, and permissions.  
Deep dives: [Section 0a: Agent model](../README.md#section-0a-agent-model-primary-subagent-agentic-workflow), [Section 0b: How workflow executes](../README.md#section-0b-how-agentic-workflow-executes-opencode-internals).

---

## Primary agents

| Agent | Mode | Tools | Java use |
|-------|------|-------|---------------|
| **Build** | `primary` | Full (edit, bash, task, skill) | Implement, run `mvn test`, apply approved refactor |
| **Plan** | `primary` | Restricted (edit/bash often `ask` or `deny`) | Triage, design, plan-only |

Switch: **Tab** or `switch_agent` keybind.

---

## Subagents

| Agent | Mode | Writes? | Use |
|-------|------|---------|-----|
| **Explore** | `subagent` | No | Fast codebase map, find usages |
| **General** | `subagent` | Yes (when allowed) | Multi-step implement, diagnose + fix |
| **Scout** | `subagent` | No | External docs, dependency source research |

Invoke: `@explore`, `@general`, `@scout` or primary delegation via `task` tool.

---

## Invocation patterns

| Pattern | Example |
|---------|---------|
| @mention | `@explore list all test classes in com.example.demo` |
| Primary orchestrates | `Plan primary — parallel triage: @explore + mvn test + …` |
| Custom command | `/triage` (see `.opencode/commands/`) |
| Skill | `Use java-junit skill to add boundary tests` |

Subagents **do not** see full chat — repeat paths and constraints in every delegation.

---

## Built-in TUI commands

| Command | Action |
|---------|--------|
| `/init` | Generate/update `AGENTS.md` |
| `/undo` | Revert last edits |
| `/redo` | Re-apply after undo |
| `/share` | Share conversation link |
| `/connect` | Provider API key |
| `Tab` | Cycle Build ↔ Plan |

Custom commands: `.opencode/commands/<name>.md` → `/name`

---

## Agent config (minimal)

`opencode.json` or `.opencode/agents/<name>.md`:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "agent": {
    "plan": {
      "mode": "primary",
      "steps": 8,
      "permission": {
        "edit": "deny",
        "bash": "ask",
        "doom_loop": "ask"
      }
    },
    "build": {
      "mode": "primary",
      "steps": 20,
      "permission": {
        "edit": "allow",
        "bash": "allow"
      }
    }
  }
}
```

Example: [opencode-agents.example.json](opencode-agents.example.json)

Create agent: `opencode agent create`

---

## Key permissions

| Key | Gates |
|-----|-------|
| `edit` | write, edit, apply_patch |
| `bash` | shell commands |
| `task` | subagent delegation |
| `skill` | SKILL.md loading |
| `doom_loop` | recovery when agent appears stuck |

Values: `allow` | `ask` | `deny`  
Bash fine-grained: `"git status*": "allow"`, `"*": "ask"`

---

## Session navigation

| Key (default) | Action |
|---------------|--------|
| **Ctrl+Down** | Enter first child session |
| **Right** / **Left** | Cycle sibling child sessions |
| **Up** | Return to parent session |

Run `/help keybinds` to see current mappings. Remap in `~/.config/opencode/keybinds.json`.

---

## Skills locations

- `.opencode/skills/<name>/SKILL.md` (project)
- `~/.config/opencode/skills/<name>/SKILL.md` (global)
- `.claude/skills/` (compatible)

---

## Java workflow mapping

| Need | OpenCode approach |
|------|-------------------|
| Codebase map | `@explore` or Plan delegates Explore |
| Run `mvn test` | Build primary bash |
| CI failure diagnosis | Plan/Build + log file + `@general` |
| Refactor after approval | Build + `@general` with pasted plan |
| Plan → implement continuity | New `@general` message with approved plan pasted |
| Repeatable triage | `/triage` custom command |
| Test conventions | `java-junit` skill |

---

## Related

- [opencode-ecosystem.md](opencode-ecosystem.md) — community repos, MCP
- [README.md](../README.md) — full reference, examples and quick prompts
- [templates/](../templates/) — customizable workflow templates
