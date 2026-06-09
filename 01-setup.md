# Section 1: Setup & CLI Basics

Before writing any AI configuration files, you need the tools installed
and an understanding of how each assistant discovers and loads context.

| Section | Topic |
|---------|-------|
| [Supported Assistants](#supported-assistants) | Config roots and CLI availability |
| [1. Cursor Setup](#1-cursor-setup) | Install, directories, context loading order |
| [2. OpenCode Setup](#2-opencode-setup) | Install, directories, CLI |
| [3. AGENTS.md](#3-universal-file-agentsmd) | Universal cross-tool project context |
| [4. Verify Setup](#4-verifying-your-setup) | Quick validation |
| [Key Concepts](#key-concepts) | How assistants discover and load context |
| [Anti-Patterns](#common-setup-anti-patterns) | Common configuration mistakes |

---

## Supported Assistants

| Assistant          | Config Root (project)   | Config Root (global)         | CLI Available |
|--------------------|-------------------------|------------------------------|---------------|
| Cursor             | `.cursor/`              | `~/.cursor/`                 | Yes (`cursor`) |
| OpenCode           | `.opencode/`            | `~/.config/opencode/`        | Yes (`opencode`) |
## 1. Cursor Setup

### Install Cursor
Download from [cursor.com](https://www.cursor.com/) or use your package manager.

### Install Cursor CLI
Cursor ships with a CLI that lets you interact with the AI from your terminal.

```bash
# Verify CLI is available
cursor --version

# Open a project
cursor /path/to/your/project

# Use the agent from the terminal (Cursor CLI)
cursor agent "Explain the architecture of this project"
```

### Key Directories
```text
your-project/
├── .cursor/
│   ├── rules/           # Persistent rules (.mdc files)
│   ├── commands/         # Custom slash commands (.md files)
│   └── skills-cursor/   # Cursor-specific skills/
├── .cursorrules          # Legacy: single-file project rules
└── AGENTS.md             # Universal project context
```

### Context Loading Order
Cursor loads context in this priority:
1. `AGENTS.md` (project root) -- always loaded
2. `.cursor/rules/*.mdc` -- loaded when glob pattern matches
3. `.cursor/commands/*.md` -- available as `/command-name`
4. Skills from `~/.agents/skills` or `.cursor/skills-cursor/` -- loaded on demand

## 2. OpenCode Setup

### Install OpenCode
```bash
# Via npm
npm install -g opencode

# Via Go
go install github.com/sst/opencode@latest

# Verify
opencode --version
```

### Key Directories
```text
your-project/
├── .opencode/
│   ├── commands/         # Custom slash commands (.md files)
│   └── config.json       # Project-level configuration
└── AGENTS.md             # Universal project context
```

Global configuration lives in `~/.config/opencode/`.

### CLI Usage
```bash
# Start interactive session
opencode

# Run a single command
opencode "Review this code for security issues"

# Run a custom command with arguments
opencode /test src/main/java/com/example/PersonService.java
```

## 3. Universal File: AGENTS.md

Both assistants recognize `AGENTS.md` at the project root.
This is the single most important file you will create -- it is loaded
on every interaction and sets the "ground truth" for your project.

```text
your-project/
└── AGENTS.md    <-- loaded by Cursor, OpenCode
```

You will learn to write this file in **Section 8**.

## 4. Verifying Your Setup

Run this checklist before proceeding:

- [ ] At least one AI assistant is installed and can open your project
- [ ] You can access the terminal/CLI for your assistant
- [ ] Your project has a `pom.xml` with Java 21 and Spring Boot 3.5+
- [ ] Git is initialized in your project directory
- [ ] You know where your assistant's config directory is (`.cursor/`, `.opencode/`)

## Key Concepts

### How Assistants Discover Context

```text
┌─────────────────────────────────────────────┐
│              AI Assistant Prompt             │
│                                             │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐ │
│  │ AGENTS.md│  │  Rules   │  │  Skills   │ │
│  │ (always) │  │ (match)  │  │ (on demand│ │
│  └──────────┘  └──────────┘  └───────────┘ │
│        │             │             │        │
│        ▼             ▼             ▼        │
│  ┌─────────────────────────────────────┐    │
│  │        Context Window (200K)        │    │
│  │  Project context + user message     │    │
│  └─────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

- **AGENTS.md**: Loaded every time. Keep it concise but complete.
- **Rules**: Loaded when a file glob matches (e.g., `*.java` triggers Java rules).
- **Skills**: Loaded only when the AI determines they are relevant.
- **Commands**: User-triggered via `/command-name`.

## Quick-Reference Cheat Sheet

| Setting                | Cursor                          | OpenCode                         |
|------------------------|---------------------------------|----------------------------------|
| Config directory       | `.cursor/`                      | `.opencode/`                     |
| Context file           | `AGENTS.md`                     | `AGENTS.md`                      |
| Skills directory       | `.cursor/skills-cursor/` or `~/.agents/skills` | —               |
| Commands directory     | `.cursor/commands/`             | `.opencode/commands/`            |
| CLI launch command     | `cursor /path/to/project`       | `opencode`                       |
| Check version          | `cursor --version`              | `opencode --version`             |

## Common Setup Anti-Patterns

**1. Not creating the config directory before starting work.**
The assistant does not create `.cursor/` or `.opencode/` for you.
If the directory is missing, rules, commands, and 09-skills.md have nowhere to live and the
assistant operates with zero project-specific context. Always run `mkdir -p` as the
very first step after cloning a repository.

**2. Mixing config formats between tools.**
Each assistant has its own file format: Cursor uses `.mdc` files for rules, OpenCode
uses plain `.md` for commands. Copying a `.mdc` file into `.opencode/commands/`
will not work — the assistant silently ignores it. Keep each tool's config directory
clean and use only the format it expects.

**3. Installing the IDE plugin but never configuring the CLI.**
The CLI is where most power-user workflows live: scripted reviews, batch processing,
CI integration. Many developers install the GUI plugin and never verify that
`cursor --version` (or equivalent) works in their terminal. This blocks you from
using agent mode, custom commands from the shell, and headless automation.

**4. Leaving default model settings unchanged.**
Out of the box, most assistants default to a general-purpose model and conservative
settings. Review your assistant's settings to select the model tier that fits your
tasks (e.g., a reasoning model for architecture work, a fast model for simple edits)
and enable features like agent mode or MCP support that are sometimes off by default.

## Next Section

Proceed to [Section 2: How AI Assistants Work](02-how-ai-assistants-work.md) to
understand the internal architecture that powers AI coding assistants.
