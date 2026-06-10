# Section 9: SKILL.md -- Building Reusable AI Capabilities

Skills are modular, reusable instruction packages that AI assistants load
**on demand** -- only when the task matches the skill's description.
Unlike `AGENTS.md` (always loaded), skills use **lazy loading**: only the
`description` field stays in context (~50 tokens per skill); the full body
(200--2000 tokens) loads when the task matches. Ten skills with lean
descriptions cost ~500 tokens idle; the same content in AGENTS.md costs
that amount on every single interaction.

| Section | Topic |
|---------|-------|
| [What is a Skill?](#what-is-a-skill) | On-demand instruction packages |
| [The SKILL.md Specification](#the-skillmd-specification) | File structure, placement per tool |
| [Anatomy of a SKILL.md](#anatomy-of-a-skillmd) | Required fields, optional fields, body |
| [Skill Design Principles](#skill-design-principles) | One job, description as trigger, output format |
| [Skill vs. AGENTS.md vs. Command](#skill-vs-agentsmd-vs-command) | When to use which |
| [Prompting Tips](#prompting-tips-for-skills) | Writing effective skill instructions |
| [Bad vs. Good Design](#bad-vs-good-skill-design-annotated-examples) | Annotated examples |
| [Skill Testing Strategy](#skill-testing-strategy) | Smoke, edge case, regression, A/B |
| [Decision Matrix](#skills-vs-agentsmd-vs-commands-decision-matrix) | Choosing the right tool |

---

## What is a Skill?

A skill is a folder containing a `SKILL.md` file with:
1. **YAML frontmatter** -- metadata that helps the AI decide when to use it
2. **Markdown body** -- detailed instructions for how to perform the task

Think of a skill as a "recipe card" for the AI. It tells the assistant
exactly how to execute a specific type of task.

## The SKILL.md Specification

The format is an open standard published at [agentskills.io](https://agentskills.io/specification),
supported by 27+ AI assistants including Cursor, OpenCode, Claude Code,
and GitHub Copilot.

### File Structure
```
skills/
├── code-review/
│   └── SKILL.md
├── generate-tests/
│   ├── SKILL.md
│   └── references/
│       └── testing-patterns.md
├── db-migration/
│   ├── SKILL.md
│   └── templates/
│       └── changelog-template.xml
└── spring-endpoint/
    └── SKILL.md
```

### Where to Place Skills

| Assistant    | Project-Level               | Global (User-Level)          |
|--------------|-----------------------------|------------------------------|
| Cursor       | `.cursor/skills-cursor/`    | `~/.agents/skills`         |
| OpenCode     | `.opencode/skills`         | `~/.config/opencode/skills` |
| Universal    | `skills/` (project root)    | varies by tool               |

## Anatomy of a SKILL.md

### Required Fields

```yaml
---
name: code-review
description: >
  Review Java code for Spring Boot best practices, SOLID violations,
  security issues, and performance problems. Use when asked to review
  code, audit a class, or check for issues.
---
```

- **name**: Unique identifier. Lowercase, hyphens, max 64 characters.
- **description**: What the skill does AND when to activate it.
  This is the most critical field -- if the AI doesn't trigger your skill,
  the description is the problem.

### Optional Fields

```yaml
---
name: code-review
description: Review Java code for best practices.
version: 1.0.0
license: MIT
compatibility:
  - cursor
  - opencode
allowed-tools:
  - read_file
  - search
  - grep
metadata:
  language: java
  framework: spring-boot
---
```

### Markdown Body

The body contains the actual instructions. Write them as if briefing
a new team member who is an expert programmer but unfamiliar with your
project's specific conventions.

```markdown
---
name: code-review
description: >
  Review Java code for Spring Boot best practices, SOLID violations,
  and security issues. Activate when asked to review, audit, or check code.
---

## Instructions

Review the provided Java code against the following checklist:

### Architecture
- [ ] Controllers only handle HTTP concerns (no business logic)
- [ ] Services contain business logic and use @Transactional where needed
- [ ] Repositories handle data access only
- [ ] DTOs are Java records, entities are never exposed via REST

### Code Quality
- [ ] No wildcard imports
- [ ] Proper annotation ordering: Lombok -> Spring -> Validation -> Docs
- [ ] Methods under 30 lines
- [ ] Meaningful variable names (no single letters except loops)

### Security
- [ ] Input validated with @Valid on @RequestBody
- [ ] No SQL concatenation (use parameterized queries)
- [ ] Sensitive data not logged
- [ ] Proper exception handling (no stack traces in API responses)

### Testing
- [ ] Test exists for the class under review
- [ ] Test naming follows: methodName_stateUnderTest_ExpectedBehavior
- [ ] Edge cases covered (null, empty, boundary values)

## Output Format

Present findings as:
1. **Critical** -- Must fix before merge
2. **Warning** -- Should fix, but not blocking
3. **Suggestion** -- Nice to have improvement

For each finding, show the line, the issue, and a fixed version.
```

## Skill Design Principles

### 1. One Skill, One Job
Each skill should do exactly one thing well. Don't create a
"do-everything" skill.

| Good                        | Bad                           |
|-----------------------------|-------------------------------|
| `code-review`               | `code-review-and-test-and-deploy` |
| `generate-tests`            | `do-all-testing-stuff`        |
| `db-migration`              | `database conventions`         |

### 2. Description Is the Trigger
The AI reads the description to decide whether to load the skill.
Write it like a search query that should match.

```yaml
# Bad -- too vague, won't trigger reliably
description: Help with code.

# Good -- specific trigger words and context
description: >
  Review Java/Spring Boot code for SOLID violations, security issues,
  and performance problems. Use when asked to review code, audit a class,
  check for issues, or do a code review.
```

### 3. Include Output Format
Tell the skill exactly what the output should look like.
Without this, every invocation produces differently structured results.

### 4. Reference Files for Large Context
If a skill needs access to large reference documents (API specs,
style guides), put them in a `references/` subfolder rather than
inlining everything in `SKILL.md`. This is lazy loading at the asset
tier: only the `description` (~50 tokens) sits in context until the
skill triggers; the agent reads `references/` files via tools only when
executing the task. A 40-line Java example inlined in `SKILL.md` costs
tokens on every turn where the skill is listed, even when unused.

```
generate-tests/
├── SKILL.md
└── references/
    ├── testing-conventions.md
    └── object-mother-examples.md
```

See [Section 7: Token Efficiency](07-prompt-optimization.md#46-token-efficiency)
for progressive disclosure tiers and the AGENTS.md vs. skill token budget.

## Skill vs. AGENTS.md vs. Command

| Feature       | AGENTS.md           | SKILL.md              | Command (.md)         |
|---------------|---------------------|-----------------------|-----------------------|
| When loaded   | Every interaction   | On demand (AI decides)| User types `/name`    |
| Purpose       | Project context     | Reusable capability   | Quick shortcut        |
| Token cost    | Always consumed     | Only when triggered   | Only when invoked     |
| Scope         | Whole project       | Specific task         | Specific action       |
| Reusable      | Per project         | Across projects       | Per project           |

## Prompting Tips for Skills

Applying prompt engineering principles (see
[Section 3: Prompting](03-prompting.md)) makes your skills trigger more
reliably and produce more consistent output:

1. **Include few-shot examples** (Principle 7) -- Add 1-2 concrete
   input-output examples in the skill body. This is the single most
   effective way to get consistent output format across invocations.
   ```markdown
   ## Example
   Input: `PersonServiceImpl.java`
   Finding:
   - **Critical**: Entity returned from controller (line 42)
   - **Fix**: Return PersonResponse record instead
   ```

2. **Assign a role** (Principle 16) -- Open the skill body with the
   persona the AI should adopt: "You are a testing specialist for
   Spring Boot applications."

3. **Use explicit task phrasing** (Principle 9) -- Start instructions
   with "Your task is" and use "You MUST" for non-negotiable requirements.
   Vague phrasing like "try to" or "consider" is treated as optional.

4. **Specify the output format** (Principle 8, 20) -- End the skill
   with the exact structure of the expected output, including headers,
   bullet format, and severity levels. Consider using an output primer
   that shows the first few lines of the expected response.

## Bad vs Good Skill Design: Annotated Examples

The difference between a skill that wastes tokens and one that delivers
consistent, actionable results comes down to specificity. Below are two
before/after pairs that illustrate the most common pitfalls.

### Example 1: Code Review Skill

**Bad version** -- vague, no structure, no examples:

```yaml
---
name: code-review
description: Review code and suggest improvements.
---
```

```markdown
Review the provided code. Look for issues and suggest improvements.
Try to cover best practices and potential bugs.
```

Why it fails:
- "suggest improvements" gives no category list -- the AI picks
  random things each time.
- No output format -- findings appear as free-form prose, impossible
  to scan or compare across reviews.
- No examples -- the AI has no calibration for the expected depth
  and tone.

**Good version** -- specific checklist, structured output, few-shot example:

```yaml
---
name: code-review
description: >
  Review Java/Spring Boot code for null-safety issues, thread-safety
  violations, SQL injection risks, and resource leaks.
  Activate when asked to review, audit, or check code.
---
```

```markdown
## Instructions

You are a senior Java engineer performing a code review.
Your task is to evaluate the provided code against this checklist:

### Checklist
- **Null safety**: Nullable parameters without validation, Optional misuse,
  bare `get()` calls on Optional
- **Thread safety**: Shared mutable state, unsynchronized collections,
  non-atomic compound operations
- **SQL injection**: String concatenation in queries, unparameterized
  native SQL, dynamic `ORDER BY` without whitelist
- **Resource leaks**: Streams, connections, or AutoCloseable instances
  not in try-with-resources blocks

## Output Format

Present each finding as:

| Field       | Description                                   |
|-------------|-----------------------------------------------|
| Severity    | `CRITICAL`, `WARNING`, or `SUGGESTION`        |
| Location    | File name and line number                     |
| Description | One-sentence explanation of the issue         |
| Fix         | Code snippet showing the corrected version    |

## Example

Input: `OrderServiceImpl.java`

| Severity   | Location                        | Description                                      | Fix                                                |
|------------|---------------------------------|--------------------------------------------------|----------------------------------------------------|
| CRITICAL   | OrderServiceImpl.java:87        | Native query built via string concatenation      | Use `@Query` with named parameter `:status`        |
| WARNING    | OrderServiceImpl.java:34        | `Optional.get()` without `isPresent()` check     | Replace with `.orElseThrow(OrderNotFoundException::new)` |
| SUGGESTION | OrderServiceImpl.java:12        | Field injection via `@Autowired`                 | Switch to constructor injection                    |
```

### Example 2: Test Generation Skill

**Bad version** -- one-liner, no constraints:

```yaml
---
name: generate-tests
description: Generate tests for the given code.
---
```

```markdown
Generate tests for the provided code. Cover edge cases.
```

Why it fails:
- No test framework specified -- the AI may produce TestNG, JUnit 4,
  or JUnit 5 depending on mood.
- No naming convention -- test method names are random.
- "Cover edge cases" is unquantified -- you might get 1 or 20.
- No structure guidance -- tests are flat, hard to read.

**Good version** -- framework, conventions, coverage targets, mocking strategy:

```yaml
---
name: generate-tests
description: >
  Generate JUnit 5 unit tests with AssertJ assertions for a Java class.
  Use when asked to create tests, write tests, or add test coverage.
---
```

```markdown
## Instructions

You are a testing specialist for Spring Boot 3.5 applications.
Your task is to generate a complete test class for the provided code.

### Framework & Dependencies
- JUnit 5 (`@ExtendWith(MockitoExtension.class)`)
- AssertJ for assertions (`assertThat(...).isEqualTo(...)`)
- Mockito for mocking (`@Mock`, `@InjectMocks`)

### Naming Convention
Method names MUST follow: `methodName_stateUnderTest_expectedBehavior`
```java
findById_existingId_returnsPerson()
findById_nonExistentId_throwsNotFoundException()
save_nullName_throwsConstraintViolation()
```

### Test Structure
Every test method MUST use given/when/then with comments:
```java
@Test
void findById_existingId_returnsPerson() {
    // given
    var personId = 42L;
    var expected = PersonMother.aDefaultPerson();
    when(repository.findById(personId)).thenReturn(Optional.of(expected));

    // when
    var result = service.findById(personId);

    // then
    assertThat(result).isEqualTo(expected);
    verify(repository).findById(personId);
}
```

### Coverage Targets
Generate the following for each public method:
1. **Happy path** -- normal successful execution
2. **Three edge cases** -- null input, empty collection, boundary value
3. **One error case** -- expected exception scenario

### Mocking Strategy
- Mock all constructor-injected dependencies with `@Mock`
- Use `@InjectMocks` on the class under test
- Prefer `when(...).thenReturn(...)` over `doReturn(...).when(...)`
- Verify important interactions with `verify(...)`

## Skill Testing Strategy

Writing a skill is only half the job. Without validation, you won't know
whether the skill triggers reliably or produces useful output.

### 1. Smoke Test

Run the skill against 3 different inputs and verify output format
compliance:

| Run | Input                   | Check                                  |
|-----|-------------------------|----------------------------------------|
| 1   | A typical service class | Output follows the defined format?     |
| 2   | A controller class      | Correct severity levels used?          |
| 3   | A repository interface  | Findings are relevant to input type?   |

If the output format varies across runs, your instructions are
ambiguous. Add an output primer or a few-shot example.

### 2. Edge Case Test

Push the skill beyond normal inputs:

- **Empty class**: Does the skill say "no issues found" gracefully?
- **Massive class** (500+ lines): Does it stay structured or degenerate
  into a wall of text?
- **Abstract class / interface**: Does it adapt or produce nonsense
  findings about missing method bodies?
- **Generated code** (MapStruct mapper): Does it waste time reviewing
  generated code, or skip it?

### 3. Regression Test ("Golden Set")

Maintain a small folder with 3-5 input/output pairs that represent
the ideal behavior:

```
skills/code-review/
├── SKILL.md
└── golden-tests/
    ├── input-01-service.java
    ├── output-01-expected.md
    ├── input-02-controller.java
    └── output-02-expected.md
```

After editing the skill, re-run it on the golden inputs and diff
the output against expectations. Major deviations mean the edit
regressed something.

### 4. A/B Comparison

Run the same prompt with and without the skill active:
1. **Without skill**: "Review PersonService.java for issues"
2. **With skill**: Same prompt, skill loaded

Compare: Is the skill version more structured? More thorough? If there's
no improvement, the skill isn't adding value -- rewrite it.

### 5. Iterate

Based on test results, refine the skill:

- **Output too verbose?** Add "Be concise. Maximum 3 sentences per finding."
- **Missing a category?** Add it to the checklist.
- **Wrong format?** Add or update the few-shot example.
- **Not triggering?** Rewrite the `description` with more trigger words.

Repeat the smoke test after each change. Skills typically need 3-5
iterations before they stabilize.

## Skills vs AGENTS.md vs Commands: Decision Matrix

| Need                                         | Use          | Why                                                   |
|----------------------------------------------|--------------|-------------------------------------------------------|
| Project-wide rules that always apply         | AGENTS.md    | Always in context, no activation needed               |
| Reusable capability triggered by task type   | Skill        | Loaded on demand, saves tokens when not needed        |
| User-triggered workflow with specific arguments | Command   | Explicit `/invoke`, supports arguments                |
| One-off instruction for a single prompt      | Inline prompt| No file needed, just type it in the chat              |

**Rules of thumb:**

- If you catch yourself pasting the same instruction in chat repeatedly
  → make it a **Skill** or a **Command**.
- If the instruction must always be active (coding style, architecture
  rules) → put it in **AGENTS.md**.
- If the instruction needs user-provided arguments (file name, entity
  name) → make it a **Command**.
- If you only need it once → just type it as an **inline prompt**.

## Quick-Reference Cheat Sheet

| SKILL.md Section       | Purpose                                                | Tips                                                     |
|------------------------|--------------------------------------------------------|----------------------------------------------------------|
| `name` (frontmatter)   | Unique identifier                                      | Lowercase, hyphens, max 64 chars                         |
| `description` (frontmatter) | Trigger condition for the AI                      | Pack with trigger words; write like a search query        |
| `version` (frontmatter)| Track changes                                          | Use semver; bump when behavior changes                   |
| `compatibility`        | Which assistants support this skill                    | Helps portability; not all tools read this field          |
| `allowed-tools`        | Restrict which tools the skill can use                 | Use to prevent unwanted file writes or shell commands     |
| Role / persona         | Who the AI should "be" while executing                 | "You are a senior Java engineer..." changes output depth |
| Instructions           | Step-by-step task description                          | Use "Your task is" and "You MUST" for non-negotiable items|
| Checklist              | Categories to evaluate                                 | Ordered by priority; use `- [ ]` for scannable lists     |
| Output format          | Structure of the response                              | Tables, severity levels, code diffs -- be explicit        |
| Few-shot example       | Concrete input → output sample                         | Single most effective way to get consistent results       |
| `references/`          | Supporting docs (specs, patterns, templates)           | Keep large reference material out of SKILL.md body        |

## Next Section

Proceed to [Section 10: Custom Commands](10-custom-commands.md) to learn
how to create `/slash` commands.
