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

## Beyond Thinking: The Full Model Landscape

Thinking/reasoning is one axis of a broader taxonomy. Understanding the full
landscape helps you make better selections.

### By Architecture

| Type | How It Works | Cost Implication | Example |
|------|-------------|------------------|---------|
| **Dense Transformer** | All parameters active per token | Predictable latency, higher per-token cost at scale | GPT-4, Claude, Llama 3 |
| **MoE (Mixture of Experts)** | Only subset of "experts" activate per token | More total params for same FLOPs; lower cost per token but higher memory | DeepSeek-V3, Mixtral, Gemini 1.5 |
| **State Space (Mamba)** | Linear-scaling recurrence (no attention) | Fast inference on long sequences; still experimental | Mamba-2, Jamba (hybrid) |

**Why this matters for development**: MoE models (DeepSeek-V3, Gemini) give you
frontier capability at lower per-token API cost but can have higher latency
variance. Dense models (Claude, GPT-4o) have more predictable performance --
better for interactive use where consistency matters.

### By Deployment Model

| Type | Latency | Cost | Privacy | Best For |
|------|---------|------|---------|----------|
| **API (closed)** | 200ms-5s | Per-token | Data leaves your infra | Interactive dev, research exploration |
| **Open-weight** | GPU-dependent | Hardware cost only | Full control | Automated pipelines, sensitive code |
| **Edge / on-device** | <100ms | Free | Complete | Autocomplete, offline, privacy-critical |

**Why this matters for research**: For heavy research (literature review, data
analysis), API models give you the best quality-to-cost ratio. For automated
pipelines running thousands of analyses daily, open-weight models
(DeepSeek-R1, Llama 3) are more economical despite requiring GPU infrastructure.

### By Training Stage (What's Been "Done" to the Model)

The training pipeline determines what a model is good at:

```
Base pretraining → Domain CT (optional) → SFT → Alignment (RLHF/DPO/CAI) → Reasoning RL (optional)
   knowledge             specialization      format      preference/refusal     chain-of-thought
```

- **Standard assistant** (Claude, GPT-4o): Base → SFT → Alignment → done
- **Reasoning model** (o3, DeepSeek-R1): Above + Reasoning RL stage
- **Code model** (DeepSeek-Coder): Domain CT on code between Base and SFT

A reasoning model is a standard assistant with an extra training stage -- not a
different architecture. Any model can become a reasoning model with the right
training.

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

### Reasoning Models (Open-Source: DeepSeek-R1, QWQ)

Open-weight reasoning models (DeepSeek-R1, QWQ) follow the same principle as
o3 -- extended chain-of-thought before answering -- but with two key differences:

- **Thinking is visible**: The reasoning trace appears inside `<think>` tags.
  You can inspect, debug, and learn from the model's reasoning process.
- **Self-hostable**: Run on your own hardware. Unlimited thinking at zero
  marginal token cost. Essential for high-volume research pipelines and
  sensitive codebases.
- **Distillation lineage**: DeepSeek-R1-Distill (based on Llama 8B/70B, Qwen
  7B/14B/32B) proves that reasoning ability can be distilled into smaller
  architectures, retaining ~80-90% of the reasoning quality at a fraction
  of the cost.

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
| Debugging concurrency (deadlocks, race conditions) | Strong + Thinking (Opus, o3) | Yes (medium-large budget) | Must reason about interleaved execution paths |
| Complex architecture review | Strong + Thinking (Opus, o3) | Yes (large budget) | Needs broad context and tradeoff analysis |
| Writing unit tests from spec | Medium (Sonnet, GPT-4o) | No | Pattern replication; CoT prompt is sufficient |
| Analyzing a failing CI pipeline | Medium + Thinking | Yes (small budget) | Needs to correlate log lines across stages |
| Optimizing a slow SQL query | Strong / Reasoning (o3, o4-mini) | Yes | Must reason about execution plans and index usage |

**How to read the table:** Start with the recommended tier. If the result
is poor, escalate one tier up or enable thinking. Never start at the top
tier for routine tasks.

---

## Development Workflow: Model Selection by Phase

Different development phases benefit from different model types.
This section maps model choices to your workflow stage.

### Phase 1: Design & Planning

Use a **reasoning model with large budget** (o3, Opus, DeepSeek-R1) when:

- Designing system architecture with multiple tradeoffs
- Analyzing failure modes and edge cases
- Planning migration strategies
- Writing ADRs (Architecture Decision Records)

The reasoning depth catches design flaws before any code is written.
A 30-second thinking phase costs ~$0.05-0.20 but can prevent hours of rework.

### Phase 2: Implementation

Use a **standard medium model** (Sonnet, GPT-4o) for:

- Generating boilerplate and CRUD operations
- Writing unit tests from specifications
- Implementing well-understood patterns (repository, factory, strategy)
- Formatting and style fixes

This is 80% of development. Standard models are fast enough and accurate enough.

### Phase 3: Debugging & Root Cause Analysis

Use **extended thinking on a standard model** (Claude with thinking) or
**reasoning model** (o4-mini) when:

- The bug spans multiple services or layers
- Reproduction is intermittent
- The stack trace makes no immediate sense
- Concurrency or timing issues are suspected

Follow this escalation pattern:
1. Standard model + full context (logs, stack trace, code) -- 70% solved here
2. Same prompt with thinking enabled (medium budget: 4096-8192) -- 25% more
3. Reasoning model (o3, R1) -- final 5%

### Phase 4: Code Review

- **Standard model** + checklist: Catches style issues, missing edge cases,
  test gaps
- **Thinking model** (small budget): When reviewing security-critical code or
  complex refactoring that touches multiple modules
- **Reasoning model**: Overkill for code review -- the benefit doesn't justify
  the cost

### Phase 5: Deployment & Incident Response

- **Standard + thinking** to analyze production logs, correlate events,
  identify root cause
- **Reasoning model** to generate runbooks, rollback plans, or post-mortems

### Phase-Specific Cost Examples

| Phase | Model | Typical Tokens | Cost Estimate |
|-------|-------|----------------|---------------|
| Architecture design | Opus + thinking (16K) | ~8K in + ~24K out | $0.25-0.50 |
| Generate CRUD controller | Sonnet 4.5 | ~2K in + ~1K out | $0.01-0.02 |
| Debug flaky test | Sonnet + thinking (4K) | ~4K in + ~6K out | $0.02-0.05 |
| Code review (PR) | GPT-4o mini | ~5K in + ~2K out | $0.001-0.003 |
| Incident analysis | Opus + thinking (8K) | ~10K in + ~8K out | $0.08-0.15 |

**Total per productive developer day**: ~$1-3 with roughly 80% standard,
15% thinking, 5% reasoning model usage.

---

## Research Workflow: Multi-Model Research Pipeline

Heavy research (literature review, data analysis, hypothesis testing)
benefits from using different models at different research stages.
No single model is optimal for all phases.

### Stage 1: Exploration & Broad Search

**Best model**: Standard model with large context (Gemini 2.5 Pro, Claude)

Upload entire papers, documentation, or codebases. Use the large context window
(~200K-2M tokens) to ingest sources in bulk -- no chunking needed.

Prompt pattern:
```
I'm researching [topic]. Given these 3 papers, extract:
1. Key claims and their evidence strength
2. Conflicting findings between papers
3. Methodological approaches used
4. Open questions they identify
```

**Cost**: Moderate -- large context means large input tokens (~$0.05-0.15 per
query). But avoids the cost and quality loss of chunking. For high-volume
research, consider self-hosting an open-weight model with long context
(Gemma 2, Llama 3 70B).

### Stage 2: Deep Analysis

**Best model**: Reasoning model with large thinking budget (o3, DeepSeek-R1)

Once you've identified a specific problem or hypothesis, use a reasoning model
to analyze it in depth. These models excel at:

- Cross-referencing claims across sources
- Identifying logical gaps and assumptions
- Extending existing work to new scenarios
- Formal reasoning (mathematics, proofs, formal methods)

Prompt pattern:
```
Given these findings from [paper A] and [paper B], analyze whether
conclusion X is justified. Consider:
- Are there alternative explanations?
- What additional experiments would confirm or refute X?
- What are the weakest assumptions in the chain?
```

For open-ended research, DeepSeek-R1 (self-hosted) is the most economical
choice -- unlimited thinking tokens at zero marginal cost. For one-off deep
dives, o3 via API gives the highest quality at $1-5 per analysis.

### Stage 3: Synthesis & Writing

**Best model**: Standard model (Claude, GPT-4o) with CoT prompting

Writing and synthesis benefit from structured output, not hidden reasoning.
Use CoT prompting to guide the structure, then let the standard model produce
clean, organized text.

Prompt pattern:
```
Write a literature review section on [topic]. Follow this structure:
1. Problem statement
2. Key approaches (list them, compare)
3. Gaps and limitations
4. How our work addresses the gaps

For each approach, state: what it does, evidence for it, limitations.
```

Synthesis is where standard models outperform reasoning models because
reasoning models tend to over-analyze and produce verbose, meandering prose
when a clean summary is needed.

### Stage 4: Verification & Reproducibility

**Best model**: Reasoning model with strict verification budget

Use reasoning models to:
- Check mathematical derivations step-by-step
- Verify statistical methodology
- Ensure code reproduces the stated results
- Validate experimental design for confounders

Prompt pattern:
```
Verify the following claim step by step:
[Claim + supporting reasoning]

For each step, state whether it follows from the previous steps.
If there is a gap, explain what's missing.
```

### Research Cost-Effectiveness Table

| Stage | Model | Why | Cost per Query |
|-------|-------|-----|----------------|
| Exploration | Gemini 2.5 Pro (2M ctx) | Ingest everything, no chunking | $0.10-0.30 |
| Deep analysis | DeepSeek-R1 (self-hosted) | Unlimited thinking, zero marginal cost per token | HW cost only |
| Deep analysis | o3 (API) | Highest reasoning quality | $1-5 per analysis |
| Synthesis | Claude Sonnet / GPT-4o | Best output quality for prose | $0.02-0.08 |
| Verification | o4-mini / DeepSeek-R1 | Lower cost, still strong reasoning | $0.01-0.10 |

### Research Multi-Model Pipeline Pattern

For serious research, use all three tiers in sequence:

```text
[Standard large-ctx model] → [Reasoning model] → [Standard writer]
     Exploration                Deep analysis         Synthesis
```

Don't try to do everything with one model. Each excels at a different part
of the research workflow. The cost of three targeted queries is often less
than one "do everything" query on a reasoning model, and the quality is
higher because each model operates in its sweet spot.

---

## Architecture Awareness for Practitioners

You don't need to understand attention mechanisms, but understanding a few
architectural properties directly affects your dev and research workflow.

### MoE vs Dense: What It Means for You

- **Dense models** (Claude, GPT-4o, Llama): Every query uses all parameters.
  Predictable latency (200-800ms), predictable cost.
- **MoE models** (DeepSeek-V3, Gemini, Mixtral): Only a fraction activates.
  Higher parameter count at lower per-token cost, but latency varies more
  (routing decisions add variance).

**Practical impact**: For interactive development (latency-sensitive), dense
models are more reliable. For batch processing (cost-sensitive), MoE wins.

### Context Window: Real-World Implications

- **128K tokens**: ~200 pages of text. Fits most single-file codebases or
  a few research papers.
- **200K tokens**: Claude 3.5/4 -- fits a medium project.
- **2M tokens**: Gemini 1.5 Pro -- fits an entire codebase. For research,
  you can dump 50+ papers into one query.

**"Lost in the middle" is real**: Models recall the beginning and end of
context better than the middle. For critical information, place it at the
start or end. Use repetition for important facts. Gemini is the least
affected by this; smaller-context models (128K) are the most affected.

### Throughput vs Latency: Choosing the Right Tradeoff

| Workload | Optimize For | Model Choice |
|----------|-------------|-------------|
| Interactive IDE chat | Latency (<500ms) | Standard, no thinking |
| Code review batch | Throughput (many files/min) | Fast model (GPT-4o mini, Haiku) |
| Architecture design | Quality (take 30s) | Thinking or reasoning |
| Automated pipeline | Cost per task | Open-weight, quantized |
| Research deep dive | Depth (minutes allowed) | Reasoning with max budget |

### Quantization Awareness

If you self-host open-weight models:
- **FP16**: Full quality, requires ~2x model size in VRAM (e.g., 70B model = ~140GB)
- **INT4/INT8**: 2-4x memory reduction, <5% quality loss. Run a 70B on a single
  consumer GPU (24GB). Sufficient for most dev/research tasks.
- **GGUF format**: Standard for CPU + GPU hybrid inference. Essential for
  running on laptops or shared servers.

Rule of thumb: INT4 quantization preserves reasoning quality well. The drop
is noticeable only on the hardest math/code benchmarks. For everyday research
and development, quantized models are the most cost-effective choice.

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

### 6. Use Self-Hosted Reasoning for High-Volume Workloads

If you run more than 500 reasoning queries per month, consider self-hosting
DeepSeek-R1 or a distilled variant. The upfront hardware cost (~$10-20K for
a multi-GPU server) breaks even against API pricing at scale, and gives you
unlimited thinking tokens.

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

### 7. Distillation as a Cost Strategy

If your team frequently runs the same type of analysis (e.g., security audit
of Spring Boot controllers), consider fine-tuning a smaller model on the
output of a reasoning model (DeepSeek-R1-Distill approach). A specialized
7B model can match a general 70B on your specific task at <5% the cost.

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
| Dev workflow           | Design→Reasoning; Implement→Standard; Debug→Thinking; Review→Standard |
| Research workflow      | Explore→Large-ctx; Analyze→Reasoning; Synthesize→Standard; Verify→Reasoning |
| Open-weight option     | DeepSeek-R1 for unlimited thinking at hardware cost only         |

---

## Quick-Reference Cheat Sheet

| Model Tier | Best For | Avoid For | Thinking Support |
|------------|----------|-----------|------------------|
| **Fast** (GPT-4o mini, Haiku 4.5) | Autocomplete, boilerplate, formatting, simple generation | Debugging, architecture, security analysis | Limited / none |
| **Medium** (Sonnet 4.5, GPT-4o, GPT-4.1) | Code generation, test writing, code review, refactoring | Deep root cause analysis, complex algorithm design | Optional (small budget) |
| **Strong** (Opus 4.6, o3) | Architecture, debugging, security audit, complex analysis | Autocomplete, boilerplate, formatting | Yes (medium-large budget) |
| **Budget Reasoning** (o4-mini, DeepSeek-R1-Distill) | Algorithm design, SQL optimization, logical reasoning | Simple generation, interactive chat | Built-in (model controls depth) |
| **Self-Hosted Reasoning** (DeepSeek-R1) | High-volume pipelines, sensitive code, unlimited thinking | Latency-sensitive interactive use | Built-in + visible thinking |

---

## Next Section

Proceed to [Section 6: Context](06-context.md) to learn context strategies
and engineering for long-horizon agent work.

Or revisit [Section 4: Prompt Techniques](04-prompt-techniques.md) for
chain-of-thought prompting patterns, or [Section 3: Prompting](03-prompting.md)
for the CO-STAR framework and 26 principles of effective prompting.
