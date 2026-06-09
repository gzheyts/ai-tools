# Section 7: Prompt Optimization & Debugging

Sections 3 and 4 taught you how to write prompts -- CO-STAR, the 26
principles, zero-shot through chain-of-thought. Section 2 explained what
happens under the hood: the agent loop, context window, context rot.

This section fills the gap between writing and maintaining prompts. It
covers what to do when a prompt **fails**, how to **measure** quality, how
to **debug** root causes, and how to **refactor** prompts that have grown
unwieldy. Everything applies to AGENTS.md, skills, and custom commands.

| Section | Topic |
|---------|-------|
| [1. Prompt Engineering Lifecycle](#1-the-prompt-engineering-lifecycle) | Draft → test → observe → fix → validate |
| [2. Taxonomy of Failures](#2-taxonomy-of-prompt-failures) | Vague outputs, ignored instructions, scope creep |
| [3. Debugging Methodology](#3-debugging-methodology) | Isolate, ablate, contrast, trace |
| [4. Optimization Workflow](#4-optimization-workflow) | Golden test set, baseline, validate |
| [5. Quality Measurement](#5-quality-measurement----what-does-better-mean) | Scoring dimensions |
| [6. Tools and Mechanics](#6-tools-and-mechanics) | In-IDE tools, external evaluators |
| [7. Applying to Config Files](#7-applying-to-agentsmd-skills-and-commands) | AGENTS.md, skills, commands |
| [8. Prompt Refactoring](#8-prompt-refactoring) | When and how to refactor |
| [9. Best Practices Summary](#9-best-practices-summary) | Consolidated checklist |
| [10. Systematic Debugging Checklist](#10-systematic-prompt-debugging-checklist) | Step-by-step debug guide |
| [11. Golden Test Set](#11-golden-test-set-best-practices) | Building and maintaining |
| [12. Failure → Fix Reference](#12-quick-reference-failure-type--debug-strategy--fix) | Quick-reference table |

---

## 1. The Prompt Engineering Lifecycle

Writing a prompt is only the first step. Production-quality prompts are
**iterated**, not authored once:

```text
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  Draft ──► Test ──► Observe ──► Diagnose ──► Fix ──► Validate│
│    ▲                                                    │    │
│    └────────────────────────────────────────────────────┘    │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

Each step has a concrete action:

| Step       | Action                                                     |
|------------|------------------------------------------------------------|
| **Draft**    | Write the prompt using CO-STAR or a template             |
| **Test**     | Run 5-10 representative requests (the "golden test set") |
| **Observe**  | Record what went wrong -- classify the failure           |
| **Diagnose** | Isolate the root cause (see Section 3)                   |
| **Fix**      | Make exactly one change to the prompt                    |
| **Validate** | Re-run the full golden test set to check for regressions |

The loop continues until every test case produces acceptable output.
The key discipline: **one change at a time**. If you change the role
assignment, the format, and add an example all at once, you cannot tell
which change fixed the problem -- or introduced a new one.

---

## 2. Taxonomy of Prompt Failures

Before fixing a prompt, name what went wrong. These six categories cover
the vast majority of failures in AI-assisted Java/Spring development.

### 2.1 Vague Outputs

The model produces something correct but generic.

**Symptom:** You ask for a repository method and get a working
`findById()`, but with no custom query, no `@Query` annotation, and
no pagination -- things you clearly need.

**Root cause:** The prompt lacks specificity. "Create a repository" does
not tell the model which methods, which query style, or which return types.

**Fix:** Add explicit requirements (Section 2, Principle 5):
```
Create PersonRepository extending CrudRepository<Person, Long>.
Include these custom query methods:
- findByLastNameIgnoreCase(String lastName) returning List<Person>
- findByEmailContaining(String fragment, Pageable pageable) returning Page<Person>
Use @Query with named parameters for the second method.
```

### 2.2 Ignored Instructions

The model skips or contradicts a constraint you specified.

**Symptom:** Your AGENTS.md says "MUST use Spring Data JDBC" but the
generated code uses JPA annotations (`@Entity`, `@Column`).

**Root cause:** One of three things:
1. The instruction is buried in the middle of a long section
   (lost-in-middle effect -- see Section 1, Section 5).
2. Another instruction contradicts it (e.g., a skill references JPA
   patterns as examples).
3. The model's training data strongly associates Spring with JPA, and
   the instruction is not forceful enough.

**Fix:** Bookend the constraint -- place it near the top AND near the
bottom of the relevant section. Use MUST/NEVER language:
```markdown
## Data Access
MUST use Spring Data JDBC (NOT JPA) for all repositories.
...
(other data access rules)
...
NEVER use javax.persistence or jakarta.persistence annotations.
Spring Data JDBC is the ONLY persistence framework for this project.
```

### 2.3 Inconsistent Behavior

The same prompt produces different output formats on different runs.

**Symptom:** Your `code-review` skill sometimes returns a markdown
table, sometimes a bullet list, sometimes inline comments.

**Root cause:** No anchoring examples. The model chooses whatever format
feels natural for the specific input, and that varies.

**Fix:** Add a few-shot example (Section 3) or an output primer:
```markdown
Present every finding in this exact format:

### Finding 1
- **Severity:** Critical | Warning | Info
- **File:** `{filename}`
- **Line:** {line number}
- **Issue:** {one-sentence description}
- **Fix:** {code suggestion}
```

### 2.4 Scope Creep

The model adds features or changes you did not ask for.

**Symptom:** You ask to "add validation to this endpoint" and the model
also refactors the service, adds a new exception class, changes the
response DTO, and restructures the test.

**Root cause:** The prompt does not constrain what the model should
NOT do. LLMs are eager to be helpful and will extend scope by default.

**Fix:** Add explicit boundaries:
```
Add @Valid to the controller method parameter and Jakarta Bean
Validation annotations to the CreatePersonRequest record.

Do NOT:
- Change any other file
- Modify the service layer
- Add exception handling (that is a separate task)
```

### 2.5 Context Rot

Accumulated, stale, or contradictory instructions in AGENTS.md.

**Symptom:** The model generates code that follows a convention you
abandoned three months ago. Or it gets confused and alternates between
two incompatible styles.

**Root cause:** AGENTS.md has grown over time without pruning. Old
rules that reference deprecated patterns remain alongside new ones.

**Fix:** Audit periodically. Use the "ask the AI" technique:
1. Ask: "What are the rules for exception handling in this project?"
2. Compare the AI's answer to your actual current conventions
3. If the AI cites an outdated rule, find and remove it from AGENTS.md

### 2.6 Hallucinated APIs

The model invents library methods or annotations that do not exist.

**Symptom:** Generated code calls `repository.findAllActive()` -- a
method that does not exist in your repository interface.

**Root cause:** The model infers what "should" exist from the context
and its training data rather than reading the actual source code. This
happens more frequently in chat mode (no tool access) than in agent
mode (which can read files).

**Fix:**
1. Ensure agent mode is active so the model can read actual source files
2. In the prompt, reference the file explicitly:
   ```
   Read PersonRepository.java first. Use ONLY the methods
   defined in that interface. Do NOT invent new query methods.
   ```
3. In AGENTS.md, add a general rule:
   ```markdown
   NEVER call methods that are not visible in the current file or
   its imports. When unsure, read the relevant interface first.
   ```

---

## 3. Debugging Methodology

When a prompt produces wrong output, resist the urge to rewrite it from
scratch. Instead, follow this four-step protocol to find the root cause
efficiently.

### Step 1: Isolate

Reproduce the failure with the **minimum context**.

- Open a fresh chat session (no prior conversation history).
- Send the prompt with only the relevant file(s) attached.
- If the failure reproduces, the problem is in the prompt itself.
- If it does not reproduce, the problem is in the surrounding context
  (AGENTS.md, open files, conversation history).

**Example:** A `generate-tests` skill produces tests that use Mockito
`@Mock` annotations instead of Spring `@MockitoBean`. In a fresh session
with only the skill attached, do you still get `@Mock`? If yes, the
skill text is the problem. If not, something in AGENTS.md or another
loaded file is conflicting.

### Step 2: Ablate

Remove prompt sections one at a time to find the culprit.

This is the prompt equivalent of binary search. If your skill has five
sections (role, constraints, examples, steps, output format), remove
one, re-test, and observe. Continue until you find the section whose
removal fixes the issue -- that section contains the bug.

**Example workflow:**

```
Full skill → wrong output
Remove output format → still wrong      (format is not the issue)
Remove examples → still wrong           (examples are not the issue)
Remove constraints → output is correct  (constraint section is the culprit)
```

Now read the constraints section carefully. You will usually find one
of the failure types from Section 2 -- a contradictory rule, an
ambiguous phrasing, or a lost-in-middle instruction.

### Step 3: Contrast

Compare a **failing** input against a **passing** input.

If the prompt works for file A but fails for file B, diff the inputs.
The difference between A and B reveals what the prompt does not handle.

**Example:** Your code-review command works on simple controllers but
produces garbage on controllers with nested `@RequestMapping` paths.
The delta: nested paths. The fix: add an example with nested paths to
the command's few-shot section.

### Step 4: Trace

Read the model's own reasoning to see where it went wrong.

- **With CoT:** Add "Think step by step before producing output" to the
  prompt temporarily. Read the reasoning trace to find the step where
  the model's logic diverges from your expectation.
- **With thinking/reasoning models:** Enable extended thinking
  (Section 4) and read the thinking block. It often reveals
  misinterpretations of your instructions that are invisible in the
  final output.

**Example CoT trace revealing the bug:**

```
Step 1: Reading the constraint "use Spring Data JDBC"
Step 2: Spring Data JDBC uses @Table annotation on entities
Step 3: I need to configure the database connection
Step 4: For Spring Data JDBC, I should use ... wait, the example in
        the skill shows @Entity which is JPA. I will follow the example.
```

The trace shows the model noticed the JDBC constraint but the skill's
example used JPA annotations, creating a contradiction. The example
won because few-shot examples override textual instructions. Fix: update
the example to use `@Table` from Spring Data JDBC.

---

## 4. Optimization Workflow

Once you can debug individual failures, apply a systematic workflow to
optimize an entire prompt artifact (AGENTS.md, skill, or command).

### 4.1 Establish a Golden Test Set

Create 5-10 representative requests that cover the full range of
behavior you expect from the prompt:

| # | Test Prompt                                            | Expected Behavior                       |
|---|--------------------------------------------------------|-----------------------------------------|
| 1 | Generate a simple CRUD endpoint for Person             | Layered arch, Java 21 records, DTO/entity split |
| 2 | Add validation to CreatePersonRequest                  | Jakarta Bean Validation, @Valid on controller |
| 3 | Generate tests for PersonService.create()              | BDD style, given/when/then, Mockito      |
| 4 | Review this controller for security issues             | Structured findings, correct severity    |
| 5 | Create a Liquibase migration to add an "active" column | XML format, rollback, precondition       |

Store these in a `test-prompts/` directory as `.md` files, one per test
case, with the expected behavior documented alongside the prompt text.

### 4.2 Measure the Baseline

Run every test case against the current prompt configuration. Save the
outputs. This is your baseline. Without it, you cannot tell whether a
change helped or hurt.

### 4.3 Change One Variable

Pick the most impactful fix from your debugging analysis and apply it.
Common single-variable changes:

- Reorder instructions (move a constraint to the top)
- Add one few-shot example
- Replace a vague word with a precise specification
- Remove a contradictory instruction
- Split a monolithic section into a separate skill

### 4.4 Validate and Commit

Re-run the full golden test set. Compare outputs to the baseline:

- **Improved:** The target test case now produces correct output
- **No regression:** All other test cases still produce acceptable output
- **Regression found:** Revert the change and try a different fix

When satisfied, commit the prompt change to Git with a descriptive
message:

```bash
git commit -m "skill(code-review): add few-shot example for nested
RequestMapping -- fixes inconsistent severity ratings on complex
controller paths"
```

### 4.5 Practical Optimization Techniques

**Instruction ordering.** The most important constraints go near the
top and are repeated near the bottom of the section. The
lost-in-middle effect (Liu et al., 2024) causes models to pay less
attention to instructions in the middle of long prompts.

```markdown
## Code Style
MUST use Java 21 features: records, sealed types, pattern matching.
...
(15 lines of specific style rules)
...
Reminder: ALL generated code MUST use Java 21 features.
```

**Constraint explicitness.** Replace soft language with hard constraints:

| Weak                     | Strong                                          |
|--------------------------|-------------------------------------------------|
| Try to use records       | MUST use records for all DTOs                   |
| Please avoid public fields | NEVER use public fields on entity classes      |
| Consider adding tests    | Generate one @Test method per public method     |

**Output anchoring.** Give the model the first line of the expected
output. This dramatically reduces format variation:

```
Generate the controller class. Start your response with:

```java
@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
public class PersonController {
```​
```

**Reducing ambiguity.** Replace subjective terms with measurable ones:

| Ambiguous                | Measurable                                     |
|--------------------------|------------------------------------------------|
| Write clean code         | Code that passes checkstyle with zero violations |
| Good test coverage       | One test per public method, covering happy path and one error case |
| Secure endpoint          | Apply @PreAuthorize, validate all inputs, return 403/404 appropriately |

**Chunking.** If a prompt exceeds ~40 lines of instructions, split it.
Move specialized instructions into skills, keep only cross-cutting
rules in AGENTS.md:

```
Before (AGENTS.md at 120 lines):
  Overview + Style + Architecture + Testing + DB + CI + Security + Review

After:
  AGENTS.md (40 lines): Overview + Style + Architecture
  skills/generate-tests/SKILL.md: Testing conventions
  skills/code-review/SKILL.md: Review checklist
  skills/db-migration/SKILL.md: Database rules
```

---

## 5. Quality Measurement -- What Does "Better" Mean?

Optimization without measurement is guessing. Define what "better"
means by evaluating along five dimensions:

| Dimension       | Question                                              | How to Check                                   |
|-----------------|-------------------------------------------------------|------------------------------------------------|
| **Correctness**   | Does the generated code compile and pass tests?     | `mvn compile` + `mvn test`                     |
| **Consistency**   | Does the same prompt produce the same result 3 times? | Run the prompt 3 times, diff the outputs       |
| **Completeness**  | Does it cover happy path AND edge cases?            | Count test cases; check for null/empty handling |
| **Conformance**   | Does it follow project conventions?                 | Checkstyle, code review against AGENTS.md rules |
| **Efficiency**    | Does it solve the task without wasted tokens?       | Check output length; is there irrelevant explanation? |

### Scoring a Prompt

For each test case in your golden test set, rate each dimension
pass/partial/fail. Track this over time to see whether your prompt
changes are trending in the right direction.

**Example scorecard:**

| Test Case              | Correct | Consistent | Complete | Conformant | Efficient |
|------------------------|---------|------------|----------|------------|-----------|
| CRUD endpoint          | pass    | pass       | pass     | partial    | pass      |
| Validation annotations | pass    | pass       | pass     | pass       | pass      |
| Test generation        | pass    | partial    | fail     | pass       | pass      |
| Security review        | pass    | pass       | partial  | pass       | fail      |

This scorecard tells you: test generation needs more examples
(inconsistent + incomplete), and the security review produces too much
irrelevant commentary (inefficient).

---

## 6. Tools and Mechanics

### 6.1 In-IDE / CLI Tools

**Cursor Chat tab.** Open a chat session (not agent mode) and paste
a prompt fragment. This tests the prompt in isolation without AGENTS.md
or open-file context contaminating the result. Useful for Steps 1-2 of
the debugging methodology.

**OpenCode `--no-context` flag.** Launches OpenCode without loading
project context files. Use this to test whether a skill works on its
own or depends on AGENTS.md rules it should not:

```bash
opencode --no-context
```

**Git diff as regression test.** After changing a prompt artifact,
commit it. Next time you run the golden test set, you can `git diff`
the prompt file to see exactly what changed and correlate it with
output differences.

### 6.2 External Evaluation Tools

**promptfoo** ([promptfoo.dev](https://promptfoo.dev)) -- the most
practical tool for prompt regression testing. It is open-source,
YAML-configured, and runs from the command line.

Install:
```bash
npm install -g promptfoo
```

Example configuration for testing a `generate-tests` skill:

```yaml
# promptfooconfig.yaml
description: "generate-tests skill evaluation"

prompts:
  - file://skills/generate-tests/SKILL.md

providers:
  - id: openai:gpt-4o
  - id: anthropic:messages:claude-sonnet-4-20250514

tests:
  - vars:
      input: "PersonService.create(CreatePersonRequest)"
    assert:
      - type: contains
        value: "@Test"
      - type: contains
        value: "given"
      - type: contains
        value: "when"
      - type: contains
        value: "then"
      - type: not-contains
        value: "@Entity"

  - vars:
      input: "OrderService.cancel(Long orderId)"
    assert:
      - type: contains
        value: "@Test"
      - type: contains
        value: "assertThrows"

  - vars:
      input: "PaymentService.process(PaymentRequest)"
    assert:
      - type: javascript
        value: "output.includes('@MockitoBean') || output.includes('@Mock')"
```

Run:
```bash
promptfoo eval
promptfoo view   # opens a web UI to compare results
```

**LangSmith** ([smith.langchain.com](https://smith.langchain.com)) --
useful when your prompts drive production flows (not just IDE
interactions). Provides tracing, dataset-based evaluation, and
regression detection.

**LLM playgrounds.** Claude.ai, OpenAI Playground, and Google AI Studio
let you iterate on prompt text rapidly before committing changes to
config files. Use them for exploratory drafting, not for systematic
testing (that is promptfoo's job).

### 6.3 Low-Tech but Effective

**PROMPTS.md scratch file.** Keep a file in the repo root where you
draft prompt iterations before committing them to AGENTS.md or a skill.
This prevents half-baked changes from affecting the team.

**test-prompts/ directory.** Store golden test cases as `.md` files:

```text
test-prompts/
├── 01-crud-endpoint.md
├── 02-validation.md
├── 03-test-generation.md
├── 04-security-review.md
└── 05-liquibase-migration.md
```

Each file contains the prompt text and the expected behavior description.

**Version tags.** When you make a significant change to AGENTS.md,
tag the section with a version comment:

```markdown
## Testing Conventions
<!-- v3 - 2026-04-10: switched from Mockito @Mock to Spring @MockitoBean -->

MUST use @MockitoBean for Spring-managed beans in @WebMvcTest classes.
...
```

This creates an audit trail so the team knows when and why a rule
changed.

---

## 7. Applying to AGENTS.md, Skills, and Commands

Each artifact type has distinct optimization patterns.

### AGENTS.md

**Keep sections focused.** Each section should cover one concern. When
a section mixes style rules with architecture rules, the model can
apply the wrong rules to the wrong context.

```markdown
## Code Style          ← naming, formatting, language features
## Architecture        ← layers, dependencies, package structure
## Testing             ← framework, conventions, naming pattern
```

**Use headers as isolation boundaries.** Models treat markdown headers
as semantic separators. A clear header helps the model retrieve the
right section for the current task.

**Bookend critical rules.** Place the most important MUST/NEVER rules
at the start AND end of each section:

```markdown
## Data Access
MUST use Spring Data JDBC for all repositories.

- Repositories extend CrudRepository or PagingAndSortingRepository
- Custom queries use @Query with named parameters
- Entity classes use @Table annotation
- All IDs use Long type

NEVER use JPA (@Entity, @Column, @OneToMany) in this project.
```

**Audit with "ask the AI."** Periodically ask the assistant: "What are
the current rules for X in this project?" Compare the answer to reality.
Discrepancies reveal stale or contradictory rules.

### Skills (SKILL.md)

**Optimize the `description:` field.** The description is what triggers
skill selection. Test it by asking the assistant: "What skill would you
use to review a database migration?" If it does not select your
`db-migration` skill, the description needs better keywords.

**Use `references/` for few-shot anchors.** Instead of embedding long
code examples in the skill body, place them in a `references/`
subdirectory and reference them:

```text
skills/
└── generate-tests/
    ├── SKILL.md
    └── references/
        ├── service-test-example.java
        └── controller-test-example.java
```

The skill body then says:
```markdown
Follow the patterns in the reference files for test structure,
naming, and assertion style.
```

**Test in isolation.** Before integrating a new or changed skill into
the full context, test it in a fresh chat session with no AGENTS.md.
This confirms the skill works independently. Then test with AGENTS.md
to check for interactions.

### Custom Commands

**Use output primers.** Commands benefit enormously from giving the model
the opening of the expected output:

```markdown
# /review command

Review the selected file for code quality issues.

Start your response with:

## Review Summary
**File:** `{filename}`
**Overall:** {PASS | NEEDS WORK | CRITICAL ISSUES}

### Findings
```

**Enforce procedural behavior with numbered steps.** Unlike free-form
09-skills.md, commands often need a specific sequence:

```markdown
# /migration command

Generate a Liquibase migration for the requested change.

Follow these steps in order:
1. Read the current schema from db/changelog/
2. Determine the next changeset ID (format: YYYYMMDD-NN)
3. Generate the XML changeset with rollback
4. Add a precondition to check for idempotency
5. Output ONLY the XML -- no explanation
```

**Test edge cases.** Commands accept `$ARGUMENTS`. Test with:
- Empty arguments (does it ask for clarification or crash?)
- Very long arguments (does it truncate or handle gracefully?)
- Malformed arguments (does it handle typos or unexpected format?)

---

## 8. Prompt Refactoring

Sometimes a prompt does not need a tweak -- it needs structural change.
Prompt refactoring applies the same principles as code refactoring:
improve structure without changing behavior.

### Signals That Refactoring Is Needed

- A single AGENTS.md section is longer than ~30 lines
- The same instruction appears in multiple places (AGENTS.md, a skill,
  and a command) -- the DRY violation makes maintenance error-prone
- Changing one part of a prompt breaks unrelated behavior (tight coupling)
- A skill's `description:` has grown into a full paragraph to handle
  too many responsibilities
- You find yourself saying "but I already told it to..." -- a sign of
  contradictory or buried instructions

### Refactoring Patterns

**Split monolith.** Extract a bloated AGENTS.md section into a dedicated
skill. The section becomes a one-line pointer:

Before:
```markdown
## Testing (45 lines)
All tests MUST follow BDD structure with given/when/then.
Use @WebMvcTest for controller tests...
Use @MockitoBean for service dependencies...
Name tests using methodName_stateUnderTest_ExpectedBehavior...
(40 more lines of testing rules)
```

After:
```markdown
## Testing
Follow the conventions in the `generate-tests` skill.
```

And `skills/generate-tests/SKILL.md` contains the full 45 lines with
proper structure, examples, and output format.

**Rename for clarity.** Replace vague instructions with precise
behavioral contracts:

| Before                          | After                                                      |
|---------------------------------|------------------------------------------------------------|
| Write good tests                | Generate one @Test method per public method in the service  |
| Follow best practices           | Use @Transactional on write operations; validate in service |
| Handle errors properly          | Throw EntityNotFoundException from service; return ProblemDetail from controller |

**Extract few-shot anchors.** Move inline examples from the skill body
into `references/` files. This makes the examples reusable across
multiple skills and easier to update when project conventions change:

Before (everything in `SKILL.md`):
```markdown
## Example
```java
@Test
void createPerson_validInput_returnsPerson() {
    // 20 lines of test code inline in the skill
}
```​
```

After:
```markdown
## Example
Follow the test pattern in references/service-test-example.java
```

**Merge duplicates.** When the same rule appears in both the root
AGENTS.md and a subdirectory AGENTS.md, consolidate. Pick one canonical
location and delete the duplicate. A common pattern:

- Cross-cutting rules (style, Java version) → root AGENTS.md
- Domain-specific rules (entity conventions, query patterns) → the
  relevant subdirectory AGENTS.md

### Worked Example

**Problem:** A 50-line "Code Review" section in AGENTS.md causes issues.
The section mixes review process instructions with severity definitions
and output format. Changes to the output format break the severity
categories.

**Refactoring steps:**

1. Copy the 50-line section into `skills/code-review/SKILL.md`
2. Structure the skill with clear subsections:
   - Role assignment
   - Review categories (architecture, style, security, tests)
   - Severity definitions with examples
   - Output format template
3. Replace the AGENTS.md section with:
   ```markdown
   ## Code Review
   Use the `code-review` skill for all code reviews.
   ```
4. Run the golden test set against the old and new configuration
5. Verify identical behavior, then commit both changes together

The result: severity definitions can now be changed without affecting
the output format, the skill can be tested in isolation, and AGENTS.md
is 48 lines shorter.

---

## 9. Best Practices Summary

| Practice                           | Why                                                         |
|------------------------------------|-------------------------------------------------------------|
| Define the failure before fixing   | Prevents random changes that may introduce new issues       |
| Change one variable at a time      | Makes it possible to identify what helped (or hurt)         |
| Keep a golden test set             | Provides a repeatable baseline for measuring improvement    |
| Commit prompt versions to Git      | Creates an audit trail; enables rollback                    |
| Use CoT/thinking to understand failures | Reveals the model's interpretation of your instructions |
| Prefer modular 09-skills.md over monolithic AGENTS.md | Easier to test, debug, and maintain independently   |
| Document why a rule exists         | Prevents accidental removal during future cleanup           |
| Audit AGENTS.md periodically       | Catches context rot before it degrades output quality       |
| Test prompts in isolation first    | Confirms the prompt works without relying on surrounding context |
| Use explicit MUST/NEVER language   | Reduces ambiguity; soft language is easily overridden       |

---

## Quick-Reference: Debugging Checklist

When a prompt produces wrong output, work through this checklist:

- [ ] **Reproduce:** Can I trigger the failure consistently?
- [ ] **Classify:** Which failure type is this? (vague, ignored, inconsistent, scope creep, context rot, hallucination)
- [ ] **Isolate:** Does it fail in a fresh session with minimum context?
- [ ] **Ablate:** Which prompt section causes the failure?
- [ ] **Contrast:** Does the prompt work for other inputs?
- [ ] **Trace:** What does the model's reasoning reveal about the root cause?
- [ ] **Fix:** One change, addressing the identified root cause
- [ ] **Validate:** Golden test set passes with no regressions
- [ ] **Commit:** Descriptive commit message explaining the fix

---

## References

1. Liu, N. F. et al. (2024). *Lost in the Middle: How Language Models
   Use Long Contexts*.
   [arxiv.org/abs/2307.03172](https://arxiv.org/abs/2307.03172)

2. Bsharat, S. M., Myrzakhan, A., & Shen, Z. (2024). *Principled
   Instructions Are All You Need*.
   [arxiv.org/abs/2312.16171](https://arxiv.org/abs/2312.16171)

3. promptfoo. *Open-source LLM testing framework*.
   [promptfoo.dev](https://promptfoo.dev/)

4. Saravia, E. *Prompt Engineering Guide*.
   [promptingguide.ai](https://www.promptingguide.ai/)

5. Anthropic. *Prompt Engineering Best Practices*.
   [docs.anthropic.com](https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering)

---

## 10. Systematic Prompt Debugging Checklist

A step-by-step checklist when a prompt, skill, or command produces bad
output. Work through it top-to-bottom; stop as soon as you find the cause.

### Step 1: Isolate -- Is the problem in the prompt, the context, or the model?

| Action | What it reveals |
|--------|-----------------|
| Try the same prompt with a **different model** (e.g., switch Claude → GPT or vice versa) | If the output changes, the model's training bias is the issue |
| Try the same prompt with **minimal context** (fresh chat, no AGENTS.md, one file) | If the output improves, context pollution (stale or contradictory rules) is the issue |
| Try a **simplified prompt** (remove everything except the core instruction) | If the output improves, prompt complexity is the issue |

### Step 2: Ablate -- Remove parts of the prompt one at a time

| Part removed | What to watch |
|--------------|---------------|
| Few-shot examples | Are they confusing the model? (e.g., JPA example when you want JDBC) |
| Role definition | Is it overconstraining? ("You are a Kotlin expert" when writing Java) |
| Format specification | Is the format too rigid? (model wastes tokens fighting the template) |
| Negative constraints (NEVER / DO NOT) | Is a negation being misinterpreted as a positive instruction? |

### Step 3: Contrast -- Create minimal passing/failing examples

1. Find the **smallest input** that fails
2. Find the **smallest input** that succeeds
3. Diff them -- what is different?

The difference reveals the gap in the prompt. For example, if a
`generate-tests` skill works for services with one dependency but fails
for services with three, the prompt likely lacks a few-shot example
showing multi-dependency mocking.

### Step 4: Trace -- Follow the agent's reasoning

| Technique | How to use it |
|-----------|---------------|
| Check which **tools were called** and in what order | In Cursor, review the agent activity log. In OpenCode, check the tool call trace. A wrong tool call order (e.g., writing before reading) reveals a prompt sequencing issue. |
| Check what **context was in the window** | If the model referenced a file it should not have, the prompt is not scoping files correctly. |
| Check if **earlier tool results polluted later reasoning** | A failed search returning irrelevant code can derail subsequent reasoning. The fix: add "If search returns no relevant results, ask the user for guidance." |

---

## 11. Golden Test Set Best Practices

### Building the Test Set

1. Start with **5-10 representative inputs** covering different task
   types (simple CRUD, validation, exception handling, pagination,
   security review)
2. Record both the **input** (prompt text + attached files) and the
   **expected output criteria** (not exact text -- behavioral checks)
3. **Version the golden set alongside the prompt** in a `test-prompts/`
   directory so they evolve together
4. **Re-run** when changing prompts, models, or context configuration
5. **Automate** with promptfoo (Section 6.2) or a simple shell script

### Concrete Golden Test Entry: `code-review` Skill

```markdown
# test-prompts/code-review/03-injection-vulnerability.md

## Input

File: `PersonController.java`
```java
@GetMapping("/search")
public List<Person> search(@RequestParam String query) {
    return jdbcTemplate.query(
        "SELECT * FROM person WHERE name = '" + query + "'",
        personRowMapper
    );
}
```​

## Expected Behavior

- [ ] Identifies SQL injection vulnerability
- [ ] Severity: Critical (not Warning, not Info)
- [ ] Recommends parameterized query or named parameters
- [ ] Does NOT suggest switching to JPA (project uses Spring Data JDBC)
- [ ] Output follows the structured format (Finding N, Severity, File, Line, Issue, Fix)
- [ ] No false positives for this simple input
```

### Maintaining the Test Set

| When to add a test case | Example |
|-------------------------|---------|
| A prompt failure was found and fixed | Add the failing input as a regression test |
| A new capability was added to the skill | Add an input that exercises the new capability |
| A model upgrade is planned | Add edge cases that stress the new model's differences |
| A team member reports unexpected output | Reproduce as a golden test case |

---

## 12. Quick-Reference: Failure Type → Debug Strategy → Fix

| Failure Type | Symptoms | Debug Strategy | Fix |
|---|---|---|---|
| **Vague output** | Correct but generic; missing project-specific details | Contrast: compare with a specific prompt that works | Add explicit requirements (methods, annotations, return types) |
| **Ignored instruction** | Model does opposite of what you asked | Ablate: find which section overrides your instruction | Bookend the constraint (top + bottom); use MUST/NEVER |
| **Inconsistent format** | Different output structure on each run | Isolate: test with minimal context to rule out interference | Add a few-shot example or output primer |
| **Scope creep** | Model changes files/layers you did not mention | Trace: check if context window contains too many files | Add explicit DO NOT boundaries |
| **Context rot** | Output follows outdated conventions | Audit: ask the AI what the current rules are | Remove stale rules from AGENTS.md; version sections |
| **Hallucinated API** | Code calls methods that do not exist | Trace: check if agent mode read the actual source file | Force file reading in prompt; add NEVER-invent constraint |
| **Contradictory output** | Model oscillates between two styles | Ablate: find the two conflicting instructions | Remove the outdated one; consolidate into one canonical rule |
| **Token waste** | Correct output buried in verbose explanation | Isolate: test if format spec is present | Add "Output ONLY the code -- no explanation" |

---

## Next Section

Proceed to [Section 8: AGENTS.md](08-agents-md.md) to write your first
project context file that shapes every AI interaction.

Or revisit earlier sections:

- [Section 3: Prompting — Management](03-prompting.md#multi-project-prompt-management)
  for storing and sharing prompts across projects
- [Section 3: Prompting](03-prompting.md) for the CO-STAR framework and
  26 principles
- [Section 4: Prompt Techniques](04-prompt-techniques.md) for zero-shot,
  one-shot, few-shot, and chain-of-thought deep dives
- [Section 2: How AI Assistants Work](02-how-ai-assistants-work.md) for
  context engineering and the agent loop

## Next Steps

- Build a golden test set for your project's most-used skill
- Run an AGENTS.md audit using the "ask the AI" technique (Section 2.5)
- Set up promptfoo for at least one skill (Section 6.2)
