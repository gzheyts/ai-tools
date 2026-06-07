# Section 5: LLM Thinking & Reasoning -- Choosing the Right Model

Section 3 covered prompting techniques like chain-of-thought where **you** tell
the model how to reason. This section covers the opposite direction: models that
**reason on their own** before producing a final answer. These models produce
dramatically better results on complex tasks -- but generate significantly more
tokens, which means slower responses and higher resource usage if applied carelessly.

Understanding the two mechanisms (extended thinking and reasoning models) and
when to use them lets you pick the right tool for every task in your
AI-assisted development workflow.

---

## Two Kinds of "Thinking" Models

There are two distinct approaches to making an LLM think before answering.
They look similar from the outside but differ in architecture and billing.

```text
┌──────────────────────────────────────────────────────────────────┐
│                  Standard Model (GPT-4o, Claude Sonnet)          │
│                                                                  │
│  Prompt ─────────────────────────────► Output                    │
│           (no intermediate reasoning)                            │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│            Extended Thinking (Claude, Gemini 2.5 Pro)            │
│                                                                  │
│  Prompt ──► [thinking block]──► Output                           │
│              (visible, billed as output tokens)                  │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│            Reasoning Model (OpenAI o1, o3, o4-mini)              │
│                                                                  │
│  Prompt ──► [hidden reasoning]──► Output                         │
│              (hidden, billed as output tokens)                   │
└──────────────────────────────────────────────────────────────────┘
```

### Extended Thinking (Anthropic Claude, Google Gemini)

The model generates a visible `thinking` block before the final answer.
You control how much it thinks via the `budget_tokens` parameter.

- Thinking tokens are billed as regular output tokens -- the per-token rate
  is the same as the base model.
- The token increase comes from **volume**: more tokens generated per request.
- You control the budget: set `budget_tokens` to 1 024 for simple tasks
  or 32 768 for complex ones.

### Reasoning Models (OpenAI o-series)

Separate model variants (o1, o3, o4-mini) that perform internal chain-of-thought
reasoning before responding. The reasoning trace is hidden from the API response
but still contributes to billing.

- Generates significantly more tokens internally compared to standard models.
- The token increase comes from both hidden reasoning **and** longer output.
- You cannot directly control the reasoning depth -- the model decides.

---

## When to Use Thinking / Reasoning Models

### Recommended Budget by Task Complexity

| Task Complexity              | Budget tokens    | Example Tasks                                    |
|------------------------------|------------------|--------------------------------------------------|
| Simple (skip thinking)       | 0                | Autocomplete, simple generation, formatting       |
| Light analysis               | 1 024 -- 2 048   | Code review, summarization, categorization        |
| Moderate reasoning           | 4 096 -- 8 192   | Debugging, refactoring analysis, test generation  |
| Complex reasoning            | 16 384 -- 32 768 | Architecture design, security audit, root cause  |
| Research-grade               | 65 536+          | Large codebase analysis, mathematical proofs      |

---

Not every task benefits from thinking. Applying reasoning models to simple
tasks wastes money and adds latency without improving quality.

### Decision Framework

| Use Case                                      | Recommended Model         | Why                                          |
|-----------------------------------------------|---------------------------|----------------------------------------------|
| Autocomplete / inline suggestions              | GPT-4o mini, Haiku 4.5   | Speed and cost; no reasoning needed          |
| Simple code generation (CRUD, getters, DTOs)   | Standard (Sonnet, GPT-4o)| Well-known patterns, zero-shot is enough     |
| Code review against a checklist                | Standard + few-shot      | Pattern matching, not reasoning              |
| Complex debugging / root cause analysis        | Thinking (medium budget)  | Multi-step trace through layers              |
| Architecture design / tradeoff analysis        | Thinking (large budget)   | Breadth and depth of analysis                |
| Math-heavy / algorithm problems                | o3 or o4-mini             | Optimized for logical/mathematical reasoning |
| Security audit with step-by-step analysis      | Thinking (large budget)   | Systematic checking of vulnerability classes |
| Refactoring a tangled method                   | Thinking (small budget)   | Needs to understand before restructuring     |
| Interactive chat in IDE                        | Standard (no thinking)    | Latency is critical for flow                 |
| Generating tests from specification            | Standard + few-shot      | Pattern replication, not reasoning           |
| Analyzing a production incident log            | Thinking (medium budget)  | Correlating events requires step-by-step     |

### The Golden Rule

> **If chain-of-thought prompting (Section 3) already gives you good results
> with a standard model, you do not need a thinking/reasoning model.**
> Thinking models are for tasks where the model's internal reasoning quality
> matters more than your ability to structure the prompt.

---

## Configuring Thinking in AI Assistants

### Cursor

Cursor lets you select the model per conversation or per request:

- **Model picker** (top of chat panel): Choose between Claude Sonnet,
  Claude Opus, GPT-4o, o3, o4-mini, and others.
- **Thinking models**: When you select a model with thinking support
  (e.g., Claude Sonnet with thinking), Cursor sends the thinking parameter
  automatically. The thinking budget is controlled by Cursor's settings.
- **Best practice**: Use a standard model for routine work. Switch to a
  thinking model when you hit a complex problem (debugging, architecture).

### OpenCode

OpenCode supports model selection via configuration:

```json
{
  "model": "claude-sonnet-4-5",
  "thinkingBudget": 8192
}
```

You can override the model per session or per command.

### Yandex SourceCraft

SourceCraft uses its own model routing. Thinking capabilities depend on
the backend model version configured for your organization. Check your
SourceCraft admin settings for available reasoning models.

### Team Guidance in AGENTS.md

Document your team's model strategy in `AGENTS.md` so every developer
uses the same approach:

```markdown
## AI Model Strategy

### Default Model
- Use Claude Sonnet 4.5 (standard, no thinking) for routine tasks:
  code generation, test writing, formatting, simple refactoring.

### When to Enable Thinking
- Enable extended thinking (budget: 4096-8192) for:
  - Debugging failures that span multiple layers
  - Architecture and design decisions
  - Security audit of controllers and services
  - Complex refactoring involving multiple classes

### When to Use Reasoning Models
- Use o3 or o4-mini for:
  - Algorithm design and optimization
  - Complex SQL query optimization
  - Mathematical or logical proofs in business rules

### When NOT to Use Thinking/Reasoning
- Autocomplete and inline suggestions (use fast model)
- Generating boilerplate (DTOs, mappers, standard endpoints)
- Simple questions and lookups
- Interactive chat where latency matters
```

---

## Cost Management Strategies

### 1. Set a Thinking Budget Proportional to Task Complexity

Do not use a 32K thinking budget for a code review. Match the budget to
the task:

```
Simple review     → budget_tokens: 1024
Debug a test      → budget_tokens: 4096
Architecture plan → budget_tokens: 16384
```

### 2. Start Without Thinking, Escalate When Needed

For most IDE interactions, start with a standard model. If the response
is shallow or misses the root cause, retry with thinking enabled. This
avoids the thinking overhead on tasks that do not benefit from it.

### 3. Use Cheaper Reasoning Models for Iteration

When iterating on a problem (trying multiple approaches), use o4-mini or
Claude Haiku with a small thinking budget. Save o3 or Opus for the final
analysis.

### 4. Batch Complex Analysis into Fewer, Larger Requests

Instead of 10 small thinking requests, consolidate into 1-2 larger requests
with a bigger budget. The per-request overhead of thinking setup is amortized
over more useful analysis.

### 5. Monitor Your Token Usage

Track your monthly API or IDE subscription usage. If usage spikes,
check whether thinking is enabled by default or whether developers are
using o1/o3 for routine tasks.

---

## Thinking Models vs Chain-of-Thought Prompting

Section 3 introduced chain-of-thought (CoT) prompting -- explicitly telling
the model to "think step by step." How does that compare to thinking models?

| Aspect                  | CoT Prompting (Section 3)     | Thinking/Reasoning Models         |
|-------------------------|------------------------------|-----------------------------------|
| Who controls reasoning  | You (via the prompt)         | The model (internally)            |
| Reasoning visible       | In the final output          | In a separate thinking block (or hidden) |
| Cost mechanism          | More output tokens (reasoning is in the answer) | Thinking tokens billed as output  |
| Works with any model    | Yes                          | Only models with thinking support |
| Quality ceiling         | Limited by prompt structure  | Model can reason beyond your prompt structure |
| Best for                | Structured, checklist tasks  | Open-ended analysis, novel problems |

**Practical combination**: Use CoT prompting to structure the problem, then
enable thinking to let the model reason deeply within that structure. This
gives you the best of both approaches -- your structure plus the model's
internal reasoning.

```
Think step by step about the order cancellation flow:
1. What states allow cancellation?
2. What happens to inventory reservations?
3. What events should be emitted?
Then implement OrderServiceImpl.cancel().
```

Sending this prompt to a thinking model means the model will:
1. Use its thinking block to reason about your structured questions.
2. Produce a higher-quality analysis than either approach alone.
3. Cost more than either approach alone -- use this combination only
   for genuinely complex tasks.

---

## Summary

| Concept                | Key Takeaway                                                     |
|------------------------|------------------------------------------------------------------|
| Extended thinking      | More tokens generated as output. You control the budget.         |
| Reasoning models       | More tokens generated internally. Model controls depth.          |
| Token multiplier       | 5 -- 20x for thinking; 10 -- 20x for legacy reasoning (o1).     |
| Default recommendation | Standard model for routine work; thinking for complex analysis.  |
| Budget strategy        | Match `budget_tokens` to task complexity. Start small, escalate. |
| Team alignment         | Document model strategy in `AGENTS.md`.                          |

---

## Model Selection Guide for Java Tasks

Use this decision table to pick the right model tier for everyday Java
development tasks. The goal is to spend the minimum required to get
a good-enough result on the first attempt.

| Task Type | Recommended Model Tier | Thinking Mode? | Why |
|-----------|------------------------|----------------|-----|
| Simple rename refactoring | Fast / cheap (GPT-4o mini, Haiku) | No | Mechanical transformation; no reasoning needed |
| Generating boilerplate CRUD (controller + service + repo) | Medium (Sonnet, GPT-4o) | No | Well-known pattern; a standard model produces correct output zero-shot |
| Writing a Liquibase / Flyway migration | Medium (Sonnet, GPT-4o) | No | Schema DDL is templated; only complex data migrations need thinking |
| Code review against a checklist | Medium / Strong (Sonnet, GPT-4.1) | Optional (small budget) | Pattern matching plus style; thinking helps catch subtle issues |
| Generating MapStruct mappers or DTOs | Fast / Medium | No | Repetitive structure; fast models handle it well |
| Debugging concurrency (deadlocks, race conditions) | Strong + Thinking (Opus, o3) | Yes (medium–large budget) | Must reason about interleaved execution paths |
| Complex architecture review | Strong + Thinking (Opus, o3) | Yes (large budget) | Needs broad context and tradeoff analysis |
| Writing unit tests from spec | Medium (Sonnet, GPT-4o) | No | Pattern replication; CoT prompt is sufficient |
| Analyzing a failing CI pipeline | Medium + Thinking | Yes (small budget) | Needs to correlate log lines across stages |
| Optimizing a slow SQL query | Strong / Reasoning (o3, o4-mini) | Yes | Must reason about execution plans and index usage |

**How to read the table:** Start with the recommended tier. If the result
is poor, escalate one tier up or enable thinking. Never start at the top
tier for routine tasks.

---

## Cost Optimization Strategies

Beyond choosing the right model, these practices reduce token usage
without sacrificing quality.

### 1. Use Fast Models for Boilerplate, Strong Models for Review

Generate code with a cheap model, then review or refine it with a stronger
one. The review pass catches errors at a fraction of the cost of generating
everything with the strongest model.

### 2. Decompose Large Tasks into Smaller Ones for Cheaper Models

A single prompt asking to "redesign the entire order module" forces a strong
model. Ten focused prompts ("add pagination to findByStatus", "extract
OrderValidator", etc.) can each use a medium model and produce better results
because context is tighter.

### 3. Use Thinking Mode Only When the Reasoning Chain Matters

Enable thinking for debugging, architecture, and security analysis -- tasks
where the model genuinely needs to reason step-by-step. Disable it for
generation, formatting, and templated output where the answer is pattern-based.

### 4. Cache Common Patterns in AGENTS.md / Skills to Reduce Token Usage

Every time the model re-discovers your project conventions, you pay for
those tokens. Encoding patterns in `AGENTS.md` or Skills means the model
reads them once per session instead of inferring them every time.

### 5. Prefer Subagents Over One Long Conversation

Long conversations accumulate context that is re-sent with every message.
Starting a fresh subagent (in Cursor: background agent; in OpenCode: `/new`)
for each independent task keeps the context window small and the per-request
cost low.

### 6. Review Token Usage Reports Weekly

Most IDE assistants and API dashboards show daily/weekly token consumption.
A 10-minute weekly review reveals whether thinking budgets are too high,
whether a specific developer is over-using o3 for routine tasks, or whether
a particular prompt is unusually token-heavy.

---

## Quick-Reference Cheat Sheet

| Model Tier | Best For | Avoid For | Thinking Support |
|------------|----------|-----------|------------------|
| **Fast** (GPT-4o mini, Haiku 4.5) | Autocomplete, boilerplate, formatting, simple generation | Debugging, architecture, security analysis | Limited / none |
| **Medium** (Sonnet 4.5, GPT-4o, GPT-4.1) | Code generation, test writing, code review, refactoring | Deep root cause analysis, complex algorithm design | Optional (small budget) |
| **Strong** (Opus 4.6, o3) | Architecture, debugging, security audit, complex analysis | Autocomplete, boilerplate, formatting | Yes (medium–large budget) |
| **Budget Reasoning** (o4-mini) | Algorithm design, SQL optimization, logical reasoning | Simple generation, interactive chat | Built-in (model controls depth) |

---

## Next Section

Proceed to [Section 8: AGENTS.md](08-agents-md.md)
to write your first project context file that shapes every AI interaction.

Or revisit [Section 4: Prompt Techniques](04-prompt-techniques.md) for
chain-of-thought prompting patterns, or [Section 3: Prompting](03-prompting.md)
for the CO-STAR framework and 26 principles of effective prompting.
