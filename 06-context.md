# Section 6: Context — Strategies and Engineering

Every token in the AI's context window competes for attention. Filling the window
with the right information makes the difference between a useful response and a
hallucinated one. This section covers what goes into context, how the window
fills up, practical strategies for Java development workflows, and agent-scale
context engineering patterns.

| Section | Topic |
|---------|-------|
| [Context vs. Prompt Engineering](#context-engineering-vs-prompt-engineering) | Scope and distinction |
| [Why Context Engineering Matters](#why-context-engineering-matters) | Attention budget, context rot |
| [Context Window Fundamentals](#context-window-fundamentals) | What fills the window, token budget |
| [What Goes Into Context](#what-goes-into-context-and-when) | Automatic, on-demand, user-provided |
| [Anatomy of Effective Context](#anatomy-of-effective-context) | Structure and prioritization |
| [Retrieval and Agentic Search](#context-retrieval-and-agentic-search) | RAG, semantic search |
| [Context Rot](#context-rot) | Symptoms and remedies |
| [Long-Horizon Strategies](#strategies-for-long-horizon-tasks) | Compaction, note-taking, subagents |
| [Context by Task Type](#context-strategy-by-task-type) | Feature, bug fix, review, migration |
| [Advanced Techniques](#advanced-context-techniques) | Priming, checkpoints |

---

## Context Engineering vs. Prompt Engineering

- **Prompt engineering** focuses on writing effective prompts (mainly system
  prompts) for one-shot tasks.
- **Context engineering** is the broader discipline of curating and maintaining
  the optimal set of tokens across all inputs — system prompts, tools, history,
  external data, MCP — especially for multi-turn, long-horizon agents.

Individual prompt quality still matters ([Section 4](04-prompt-techniques.md));
context quality is what makes it scale.

> Source for agent-scale patterns:
> [Anthropic — Effective Context Engineering for AI Agents](https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents)
> (Sep 2025)

---

## Why Context Engineering Matters

- LLMs have a finite **attention budget** — as context grows, recall and precision
  degrade (**context rot**).
- The transformer architecture creates n² pairwise relationships between tokens,
  making focus harder at scale.
- Longer sequences are underrepresented in training data, further degrading
  long-context performance.

The key insight for day-to-day work: as conversation history grows, the space
available for reasoning shrinks. This is why long conversations degrade.

---

## Context Window Fundamentals

### What Is the Context Window?

The context window is the total amount of text (measured in tokens) that the
LLM can process in a single request. Everything the assistant knows during a
conversation must fit in this window:

- System prompt (AGENTS.md, active skills, tool descriptions)
- Conversation history (your messages + AI responses)
- Tool call results (file contents, grep output, shell output)
- The AI's own reasoning (thinking/chain-of-thought tokens)

### Context Window Sizes (2025–2026)

| Model Family        | Context Window | Approx. Lines of Code | Notes                    |
|---------------------|----------------|------------------------|--------------------------|
| GPT-4o              | 128K tokens    | ~4,000 lines           | Most common default      |
| Claude Sonnet 4     | 200K tokens    | ~6,500 lines           | Large window             |
| Claude Opus 4       | 200K tokens    | ~6,500 lines           | Premium tier             |
| Gemini 2.5 Pro      | 1M tokens      | ~32,000 lines          | Largest available        |
| Local models (8B)   | 8–32K tokens   | ~250–1,000 lines       | Significantly smaller    |

### The Token Budget

Not all tokens are equal. A rough budget for a typical coding session:

| Component               | Typical Size    | % of 128K Window |
|--------------------------|-----------------|-------------------|
| System prompt + AGENTS.md| 2–5K tokens    | 2–4%              |
| Tool descriptions        | 3–8K tokens    | 2–6%              |
| Active skill             | 1–3K tokens    | 1–2%              |
| File contents (reads)    | 5–40K tokens   | 4–31%             |
| Conversation history     | 10–60K tokens  | 8–47%             |
| AI response              | 1–8K tokens    | 1–6%              |
| **Available for reasoning** | **remainder** | **varies**       |

---

## What Goes Into Context and When

### Automatic Context (Always Present)

These are loaded by the assistant at the start of every request:

| Source              | When Loaded                    | Can You Control It?           |
|---------------------|--------------------------------|-------------------------------|
| System prompt       | Every request                  | No (set by assistant vendor)  |
| AGENTS.md           | Every request                  | Yes — edit content and length |
| Tool descriptions   | Every request                  | No (built-in tools are fixed) |
| MCP tool descriptions| Every request if configured   | Yes — add/remove MCP servers  |
| Open file tabs      | Varies by assistant            | Yes — close irrelevant tabs   |

### On-Demand Context (Loaded When Needed)

| Source                | When Loaded                              | Token Cost        |
|-----------------------|------------------------------------------|-------------------|
| File reads (Read tool)| When AI decides to read a file           | Proportional to file size |
| Grep/search results   | When AI searches for something           | Proportional to matches   |
| Shell command output   | When AI runs a command                  | Proportional to output    |
| MCP tool results       | When AI calls an MCP tool               | Proportional to response  |
| Skill content          | When assistant selects a matching skill | 1–3K tokens typically     |
| Subagent results       | When a subagent returns                 | Proportional to response  |

### Context You Should Provide

| Task Type              | Essential Context                        | Nice to Have                |
|------------------------|------------------------------------------|-----------------------------|
| New feature            | User story, relevant entities, AGENTS.md | Similar existing feature    |
| Bug fix                | Error message, stack trace, relevant code | Git diff, recent changes   |
| Code review            | The diff, project conventions            | Test coverage status        |
| Refactoring            | Current code, target pattern             | Examples of desired pattern |
| Database migration     | Current schema, desired change           | Existing migrations         |

---

## Anatomy of Effective Context

The goal is to find the **smallest set of high-signal tokens** that maximizes
the likelihood of the desired outcome:

- **System prompts** — operate at the right "altitude": not too brittle/hardcoded,
  not too vague. Use XML tags or Markdown headers to organize sections clearly.
- **Tools** — self-contained, unambiguous, and minimal. Overlapping or bloated tool
  sets cause agent confusion.
- **Few-shot examples** — a small, diverse, canonical set rather than an exhaustive
  list of edge cases.

Keep [AGENTS.md](09-agents-md.md) concise; extract verbose guidance into
[skills](10-skills.md).

---

## Context Retrieval and Agentic Search

- **Just-in-time retrieval**: agents maintain lightweight references (file paths,
  URLs, queries) and load data dynamically at runtime, rather than pre-loading
  everything.
- This mirrors human cognition — we use indexes and bookmarks, not full memorization.
- Enables **progressive disclosure**: agents incrementally discover context through
  exploration.
- Trade-off: runtime exploration is slower than pre-computed retrieval → **hybrid
  strategies** are often optimal.

In practice: tell the AI which files to read instead of opening many tabs.

> "Read PersonController.java and PersonService.java, then generate a new
> search method following the same patterns."

---

## Context Rot

Context rot is the progressive degradation of AI output quality as a
conversation grows longer. It happens because:

1. **Important instructions get pushed up** — AGENTS.md and early prompts drift
   far from the current generation position
2. **Contradictory context accumulates** — earlier attempts and their fixes
   create conflicting signals
3. **"Lost in the middle" effect** — LLMs pay more attention to the beginning and
   end of the context, less to the middle
4. **Stale file contents** — files read 10 messages ago may have changed

### Symptoms of Context Rot

| Symptom                                  | What's Happening                        |
|------------------------------------------|-----------------------------------------|
| AI forgets AGENTS.md conventions         | Instructions pushed too far from cursor |
| AI repeats earlier mistakes              | Conflicting fix attempts in history     |
| AI hallucinates non-existent methods     | Stale file reads in context             |
| Output quality suddenly drops            | Context window is near-full             |
| AI ignores your latest instruction       | Lost in the middle of a long context    |

### Remedies

| Strategy                      | How                                              | When to Use                  |
|-------------------------------|--------------------------------------------------|------------------------------|
| **Start a new chat**         | Fresh context, re-state the task — see [13-agent-sessions.md](13-agent-sessions.md) for specific split triggers | Every 10–15 messages |
| **Use subagents**            | Each subagent gets a fresh window — see [13-agent-sessions.md](13-agent-sessions.md) for child session mechanics | Multi-phase workflows |
| **Close irrelevant tabs**    | Reduce automatic context noise                   | Before starting a new task   |
| **Summarize progress**       | "So far we've done X. Now do Y."                 | When continuing a long task  |
| **Re-attach key files**      | Reference the critical file again                | When AI forgets file contents|
| **Keep AGENTS.md concise**   | Extract verbose sections into skills             | Always                       |

---

## Strategies for Long-Horizon Tasks

Three techniques for managing tasks that exceed the context window:

### Compaction

- Summarize the conversation history and reinitiate a new context window with
  the compressed summary.
- Balance **recall** (keep critical details) vs. **precision** (discard redundant
  content).
- Lightest-touch version: clearing old tool call results from history.

For the full compaction treatment — trigger threshold, what survives, what is discarded,
and recovery steps — see [13-agent-sessions.md — Compaction](13-agent-sessions.md).

### Structured Note-Taking

- The agent writes notes to external memory (e.g., a `NOTES.md` file, a to-do list)
  and pulls them back in later.
- Provides persistent memory with minimal context overhead across long tasks.

For a concrete implementation of this pattern across agent sessions, see
[`WORKFLOW_STATE.md` in 13-agent-sessions.md](13-agent-sessions.md).

### Sub-Agent Architectures

- A lead agent coordinates high-level planning; specialized sub-agents handle
  focused tasks with clean context windows.
- Sub-agents may consume tens of thousands of tokens internally but return only a
  **condensed summary** (1,000–2,000 tokens) to the lead agent.
- Achieves clear separation of concerns and enables parallel exploration.

For child session mechanics and parallel session semantics, see
[13-agent-sessions.md — Child Session Mechanics](13-agent-sessions.md).

See [Section 12: Agents & Subagents](12-agents-subagents.md) and
[opencode-agent-patterns/](opencode-agent-patterns/) for orchestration patterns.

---

## Context Strategy by Task Type

### Feature Implementation

```text
Open files:
  - AGENTS.md (always)
  - Relevant entity/model (1 file)
  - Similar existing feature (1 file for reference)
  - Target package (for conventions)

Close:
  - Unrelated test files
  - CI/CD configs
  - Unrelated modules

Strategy:
  - Phase 1: Read entity + similar feature → generate new code
  - Phase 2: New chat → generate tests (fresh context)
  - Phase 3: New chat → review all changes (fresh context)
```

### Bug Fix / Debugging

```text
Open files:
  - AGENTS.md (always)
  - The failing test or error location
  - The code being tested

Paste into chat:
  - Full error message and stack trace
  - Relevant log output

Close:
  - Everything else

Strategy:
  - Single focused chat
  - Provide error context upfront
  - Ask for diagnosis before fix
```

### Code Review

```text
Open files:
  - AGENTS.md (always)
  - The diff (paste or let the AI read via git)

Close:
  - Everything else (let the AI read what it needs)

Strategy:
  - Single chat with the diff
  - Use the code-review skill for consistent output
  - Keep context clean — don't mix review with implementation
```

### Database Migration

```text
Open files:
  - AGENTS.md (always)
  - db/changelog/db.changelog-master.xml
  - Recent migration for pattern reference

Close:
  - Java source files (not needed for migration itself)

Strategy:
  - Single focused chat
  - If MCP postgres is available, let AI inspect current schema
  - Generate migration → review → generate rollback
```

---

## Advanced Context Techniques

### Context Priming

Before a complex task, prime the context with a summary:

> "I'm working on the demo-service project. It's a Spring Boot 3.5 app with
> Java 21, Spring Data JDBC, PostgreSQL, and Liquibase. I need to add a
> search endpoint for persons. The existing GET /api/persons endpoint is in
> PersonController.java."

This is especially useful in assistants that don't auto-load AGENTS.md.

### Context Checkpoints

In long tasks, periodically checkpoint:

> "Let me summarize what we've done: (1) created the migration for the
> status column, (2) updated the entity. Now generate the DTO and mapper."

This reinforces important context at the end of the window where the LLM
pays the most attention.

---

## Core Guiding Principle

> *Find the smallest possible set of high-signal tokens that maximize the
> likelihood of the desired outcome.*

As models improve, they require less prescriptive engineering — but treating
context as a **precious, finite resource** will remain fundamental to building
reliable agents.

---

## Quick-Reference Cheat Sheet

| Concept                | One-Line Summary                                          |
|------------------------|-----------------------------------------------------------|
| Context engineering    | Curate all tokens (prompts, tools, history, MCP) for agents |
| Context window         | Total tokens the LLM can process in one request           |
| Token budget           | How the window is divided: system + history + tools + response |
| High-signal tokens     | Smallest set that maximizes desired outcome               |
| Context rot            | Quality degradation in long conversations                 |
| Lost in the middle     | LLMs attend less to the middle of long contexts           |
| Just-in-time retrieval | Load references dynamically, not everything upfront       |
| Compaction             | Summarize history to free context window                  |
| Structured notes       | External memory (NOTES.md) pulled in when needed          |
| Context priming        | Summarize project/task at the start of a chat             |
| Context checkpoint     | Summarize progress to reinforce important context         |
| New chat strategy      | Start fresh every 10–15 messages or between task phases   |
| Subagent isolation     | Each subagent gets a fresh context window                 |

---

## Next Section

Proceed to [Section 7: Prompt Optimization & Debugging](07-prompt-optimization.md)
to learn how to debug and improve your prompts systematically.

### Further Reading

- [Section 2: How AI Assistants Work](02-how-ai-assistants-work.md) — agent loop
  and how context is assembled
- [Section 4: Prompt Techniques](04-prompt-techniques.md) — prompting best
  practices and production patterns
- [Section 9: AGENTS.md](09-agents-md.md) — writing concise project context
- [Section 12: Agents & Subagents](12-agents-subagents.md) — context-fresh
  subagent workflows
- [13-agent-sessions.md](13-agent-sessions.md) — session lifecycle, compaction details,
  WORKFLOW_STATE.md handoff, when to split sessions
- [Section 14: MCP Servers](14-mcp-servers.md) — controlling MCP tool context cost
