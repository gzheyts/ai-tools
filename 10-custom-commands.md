# Section 10: Custom Slash Commands

Custom commands are markdown files that you trigger by typing `/command-name`
in your AI assistant's chat. They are the fastest way to automate repetitive
tasks -- one keystroke launches a predefined prompt.

| Section | Topic |
|---------|-------|
| [Commands vs. Skills](#commands-vs-skills) | Trigger, discovery, and scope comparison |
| [Where to Place Commands](#where-to-place-commands) | Project-level and global paths per tool |
| [Format by Assistant](#format-by-assistant) | Cursor, OpenCode syntax |
| [Practical Commands for Java](#practical-commands-for-java-development) | `/review`, `/test`, `/endpoint`, `/migration` |
| [Tips for Effective Commands](#tips-for-effective-commands) | Focus, context, testing, sharing |
| [Command Design Patterns](#command-design-patterns) | Scaffolding, analysis, migration, debug, docs |
| [Argument Handling](#argument-handling-across-tools) | How arguments work per assistant |

---

## Commands vs. Skills

| Feature          | Skill (SKILL.md)             | Command (.md file)          |
|------------------|------------------------------|-----------------------------|
| Triggered by     | AI decides automatically     | User types `/name`          |
| Discovery        | AI reads description         | Listed in `/` menu          |
| Arguments        | No (context from chat)       | Yes (OpenCode)              |
| Frontmatter      | Required (name, description) | Optional (depends on tool)  |
| Best for         | Complex, multi-step tasks    | Quick, focused actions      |

## Where to Place Commands

| Assistant    | Project-Level             | Global (User-Level)           |
|--------------|---------------------------|-------------------------------|
| Cursor       | `.cursor/commands/`       | `~/.cursor/commands/`        |
| OpenCode     | `.opencode/commands/`     | `~/.config/opencode/commands/`|

The filename (minus `.md`) becomes the command name:
- `code-review.md` -> `/code-review`
- `generate-tests.md` -> `/generate-tests`
- `explain.md` -> `/explain`

## Format by Assistant

### Cursor Commands

Cursor uses **plain markdown** with no frontmatter.

```
.cursor/commands/
├── review.md
├── test.md
├── refactor.md
└── explain.md
```

**Example: `.cursor/commands/review.md`**
```markdown
Review the currently open file (or the file I specify) for:

1. Spring Boot architecture violations:
   - Business logic in controllers
   - Direct repository access from controllers
   - Entities exposed in REST responses

2. Java 21 best practices:
   - Use records for DTOs
   - Use sealed classes where appropriate
   - Pattern matching instead of instanceof chains

3. Security:
   - Input validation with @Valid
   - No SQL string concatenation
   - Sensitive data not logged

Present findings grouped by severity: Critical, Warning, Suggestion.
For each finding, show the problematic line and a corrected version.
```

Usage in Cursor chat:
```
/review
/review PersonService.java
```

### OpenCode Commands

OpenCode supports **YAML frontmatter** with metadata and argument placeholders.

**Example: `.opencode/commands/review.md`**
```markdown
---
description: Review Java code for Spring Boot best practices
---

Review $ARGUMENTS for:

1. Architecture violations (SOLID, layer boundaries)
2. Java 21 idioms (records, sealed classes, pattern matching)
3. Security issues (input validation, SQL injection)
4. Test coverage gaps

Output findings as: Critical > Warning > Suggestion.
Show the problematic code and a fixed version for each finding.
```

**Argument Substitution (OpenCode-specific):**
- `$ARGUMENTS` -- everything typed after the command name
- `$1`, `$2` -- positional arguments
- `` !`command` `` -- inject shell command output

**Example: `.opencode/commands/test.md`**
```markdown
---
description: Generate tests for a Java class
---

Generate JUnit 5 tests for $1.

Use these conventions:
- Test class name: {ClassName}Test
- Method naming: methodName_stateUnderTest_ExpectedBehavior
- Use ObjectMother pattern for test data
- Extend AbstractComponentTest for integration tests
- BDD structure: given / when / then

Current test dependencies (from pom.xml):
!`grep -A2 'test' pom.xml | head -20`
```

## Practical Commands for Java Development

### `/review` -- Code Review
```markdown
Review the specified file against project conventions:
- Architecture: layered boundaries, DTO usage, no entity exposure
- Style: import order, annotation order, naming conventions
- Security: input validation, parameterized queries, error handling
- Performance: N+1 queries, unnecessary object creation, stream misuse

Group findings by severity. Show code diffs for each fix.
```

### `/test` -- Generate Tests
```markdown
Generate JUnit 5 tests for the specified class.

Requirements:
- Test class extends AbstractComponentTest
- Method naming: methodName_stateUnderTest_ExpectedBehavior
- Use ObjectMother for test data builders
- Cover: happy path, null inputs, empty collections, boundary values
- Use @Transactional on integration tests
- Mock external dependencies with @MockBean
```

### `/endpoint` -- Generate REST Endpoint
```markdown
Generate a complete REST endpoint for the specified entity:

1. Controller (@RestController) with OpenAPI annotations
2. Service interface + ServiceImpl
3. Repository (Spring Data JDBC)
4. Request/Response DTOs (Java records)
5. MapStruct mapper
6. JUnit 5 test class

Follow the layered architecture from AGENTS.md.
Use existing patterns from the codebase as reference.
```

### `/migration` -- Database Migration
```markdown
Generate a Liquibase changelog for the requested schema change.

Requirements:
- XML format
- Include rollback statements
- Use PostgreSQL-compatible types
- Add Oracle-compatible alternatives in comments
- Follow naming: YYYY-MM-DD-NNN-description.xml
- Include proper author and context attributes
```

### `/explain` -- Code Explanation
```markdown
Explain the specified code in detail:

1. **Purpose**: What does this code do and why?
2. **Flow**: Step-by-step execution flow
3. **Dependencies**: What does it depend on?
4. **Patterns**: What design patterns are used?
5. **Edge Cases**: What could go wrong?

Use simple language. Include a sequence diagram if the code involves
multiple service calls.
```

### `/refactor` -- Suggest Refactoring
```markdown
Analyze the specified code and suggest refactoring improvements:

1. SOLID principle violations
2. Opportunities for Java 21 features (records, sealed classes, pattern matching)
3. Unnecessary complexity (KISS violations)
4. Duplicated logic (DRY violations)
5. Performance improvements

For each suggestion, show the current code and the proposed change.
Explain why the change is beneficial.
```

## Tips for Effective Commands

### Keep Commands Focused
One command = one action. Don't combine review + test + deploy into
a single command.

### Use Project Context
Commands work best when they reference `AGENTS.md` conventions:
```markdown
Follow the coding conventions documented in AGENTS.md.
Use the project's testing patterns and naming conventions.
```

### Test Your Commands
After creating a command:
1. Type `/command-name` in your assistant
2. Verify it appears in the suggestions
3. Run it and check the output quality
4. Refine the instructions based on results

### Share Commands via Git
Since commands live in the project directory, they are versioned with
your code. The entire team gets the same commands:
```bash
git add .cursor/commands/ .opencode/commands/
git commit -m "Add AI assistant custom commands for team workflows"
```

## Prompting Tips for Commands

Applying prompt engineering principles (see
[Section 3: Prompting](03-prompting.md)) makes your commands produce
higher-quality, more consistent output:

1. **Use chain-of-thought** (Principle 12) -- For review and analysis
   commands, add "think step by step" to force the model to reason
   through each category before producing findings. This prevents
   superficial responses.
   ```markdown
   Think step by step through each category:
   1. First, check architecture layer boundaries
   2. Then, examine security patterns
   3. Finally, assess code style compliance
   ```

2. **Assign a persona** (Principle 16) -- Open the command with the
   role: "You are a senior Java engineer performing a code review."
   This changes the depth and vocabulary of the response.

3. **Use output primers** (Principle 20) -- End command instructions
   with the first few lines of the expected output format. This anchors
   the response structure and prevents the model from inventing
   its own format every time.

4. **Break multi-step tasks into numbered steps** (Principle 3) --
   For commands like `/endpoint` that produce multiple files, list
   the files in dependency order and instruct the model to produce
   them sequentially.

## Command Design Patterns

Five proven patterns cover the majority of commands a Java team needs.
Each pattern below includes a description, when to use it, and the
actual command file content in Cursor format.

### Pattern 1: Scaffolding Command (`/endpoint`)

**When to use:** Starting a new feature that follows a repeatable
structure (endpoint, event listener, scheduled job).

**`.cursor/commands/endpoint.md`**
```markdown
You are a Spring Boot architect. Generate a complete REST endpoint
for the entity: $ARGUMENTS

Produce the following files in this order:

1. **DTO** (`{Entity}Request.java`, `{Entity}Response.java`)
   - Java records
   - Jakarta Validation annotations on request fields

2. **Mapper** (`{Entity}Mapper.java`)
   - MapStruct interface with `@Mapper(componentModel = "spring")`

3. **Repository** (`{Entity}Repository.java`)
   - Spring Data JDBC `CrudRepository<{Entity}, Long>`

4. **Service** (`{Entity}Service.java` interface + `{Entity}ServiceImpl.java`)
   - Interface with Javadoc on each method
   - Impl with `@Service`, `@Transactional`, constructor injection

5. **Controller** (`{Entity}Controller.java`)
   - `@RestController`, `@RequestMapping("/api/v1/{entities}")`
   - OpenAPI `@Operation` and `@ApiResponse` annotations
   - `@Valid` on request bodies
   - Returns `ResponseEntity<{Entity}Response>`

6. **Test** (`{Entity}ServiceImplTest.java`)
   - JUnit 5, Mockito, AssertJ
   - Method naming: methodName_stateUnderTest_expectedBehavior
   - given/when/then structure

Follow the layered architecture from AGENTS.md.
Use existing codebase patterns as reference.
```

### Pattern 2: Analysis Command (`/review`)

**When to use:** Before a merge request, or when inheriting unfamiliar
code.

**`.cursor/commands/review.md`**
```markdown
You are a senior Java engineer performing a code review.
Review the specified file or selection step by step.

Think through each category before producing findings:

### 1. Architecture
- Layer boundary violations (business logic in controllers?)
- Entity exposure through REST API
- Proper use of DTOs and mappers

### 2. Security
- Input validated with @Valid
- Parameterized queries (no SQL concatenation)
- Sensitive data not logged or returned in responses

### 3. Reliability
- Null safety (Optional usage, @Nullable annotations)
- Resource management (try-with-resources)
- Exception handling (no swallowed exceptions)

### 4. Performance
- N+1 query patterns
- Unnecessary object creation in loops
- Stream misuse (parallel streams on small collections)

## Output Format

| Severity   | Location           | Issue               | Fix                  |
|------------|--------------------|---------------------|----------------------|
| CRITICAL   | File:line          | Description         | Code snippet         |
| WARNING    | File:line          | Description         | Code snippet         |
| SUGGESTION | File:line          | Description         | Code snippet         |

Produce a summary at the end: total findings by severity and an
overall assessment (APPROVE / REQUEST CHANGES).
```

### Pattern 3: Migration Command (`/migration`)

**When to use:** Any schema change -- adding columns, tables, indexes,
or constraints.

**`.cursor/commands/migration.md`**
```markdown
You are a database migration specialist.
Generate a Liquibase XML changelog for the following change:

$ARGUMENTS

Requirements:
- Format: XML (Liquibase 4.x schema)
- File name: use today's date as `YYYY-MM-DD-NNN-description.xml`
- Include `<rollback>` for every `<changeSet>`
- Use PostgreSQL-compatible column types
- Add Oracle-compatible alternatives in XML comments
- Set `author` to "developer" and generate a unique `id`
- Add `<preConditions onFail="MARK_RAN">` where appropriate
  (e.g., check table/column doesn't already exist)

Output ONLY the XML file content. Do not add explanations outside
the XML block.
```

### Pattern 4: Debugging Command (`/debug`)

**When to use:** A test is failing, an error appears in logs, or
behavior is unexpected.

**`.cursor/commands/debug.md`**
```markdown
You are a debugging specialist for Spring Boot applications.
Investigate the following error or failing test:

$ARGUMENTS

Follow this process:
1. **Reproduce**: Identify the exact error message and stack trace
2. **Locate**: Find the code responsible for the error
3. **Analyze**: Read the relevant method and its dependencies
4. **Hypothesize**: List 2-3 possible root causes, ranked by likelihood
5. **Fix**: Propose a concrete fix for the most likely cause
6. **Verify**: Explain how to confirm the fix works

## Output Format

### Error Summary
One-line description of what went wrong.

### Root Cause Analysis
| # | Hypothesis             | Likelihood | Evidence                    |
|---|------------------------|------------|-----------------------------|
| 1 | Most likely cause      | HIGH       | What points to this cause   |
| 2 | Alternative cause      | MEDIUM     | What points to this cause   |

### Proposed Fix
Show the exact code change (before → after).

### Verification
Steps to confirm the fix resolves the issue.
```

### Pattern 5: Documentation Command (`/explain`)

**When to use:** Onboarding to unfamiliar code, writing Javadoc, or
preparing a knowledge-transfer document.

**`.cursor/commands/explain.md`**
```markdown
You are a technical writer specializing in Java/Spring Boot codebases.
Explain the specified code section in detail.

## Output Structure

### Purpose
What does this code do, and why does it exist? (2-3 sentences)

### Execution Flow
Step-by-step walkthrough, numbered. Include which Spring beans are
called and in what order.

### Dependencies
| Dependency           | Type                | Purpose                     |
|----------------------|---------------------|-----------------------------|
| Class/interface name | Injected / Extended | What it provides             |

### Design Decisions
What patterns or trade-offs are visible in the code? Why was this
approach chosen over alternatives?

### Edge Cases & Risks
- What happens if X is null?
- What happens under concurrent access?
- What happens if an external service is unavailable?

Include a Mermaid sequence diagram if the code involves 3+ service
calls.
```

## Argument Handling Across Tools

Different assistants handle command arguments differently. Here is the
same `/migration` command adapted for each tool, showing how the user's
input "add email column to persons table" reaches the prompt.

### Cursor

Cursor uses `$ARGUMENTS` or `{{input}}` as a placeholder. The user
types the argument after the command name.

**`.cursor/commands/migration.md`**
```markdown
Generate a Liquibase XML changelog for: $ARGUMENTS

Requirements:
- XML format, Liquibase 4.x schema
- Include <rollback> for every <changeSet>
- PostgreSQL-compatible types
- File name: YYYY-MM-DD-NNN-description.xml
```

Usage:
```
/migration add email column to persons table
```

### OpenCode

OpenCode supports `$ARGUMENTS` (full text) and positional `$1`, `$2`
in the frontmatter/body.

**`.opencode/commands/migration.md`**
```markdown
---
description: Generate a Liquibase changeset from a schema change description
---

Generate a Liquibase XML changelog for: $ARGUMENTS

Requirements:
- XML format, Liquibase 4.x schema
- Include <rollback> for every <changeSet>
- PostgreSQL-compatible types
- File name: YYYY-MM-DD-NNN-description.xml
```

Usage:
```
/migration add email column to persons table
```


## Quick-Reference Cheat Sheet

| Command       | Purpose                                     | Arguments                        | Output                                  |
|---------------|---------------------------------------------|----------------------------------|-----------------------------------------|
| `/review`     | Analyze code for issues                     | File name or selection           | Findings table (Critical/Warning/Suggestion) |
| `/test`       | Generate unit tests                         | Class name                       | JUnit 5 test class                      |
| `/endpoint`   | Scaffold a full REST endpoint               | Entity name                      | Controller + Service + Repo + DTO + Test |
| `/migration`  | Generate a Liquibase changeset              | Schema change description        | Liquibase XML file                      |
| `/explain`    | Explain code purpose and design             | File name or selection           | Structured markdown documentation       |
| `/refactor`   | Suggest refactoring improvements            | File name or selection           | Before/after code diffs with rationale  |
| `/debug`      | Investigate a failing test or error         | Error message or test name       | Root cause analysis + proposed fix      |

## Next Section

Proceed to [Section 11: Agents & Subagents](11-agents-subagents.md) to learn
how to orchestrate multiple specialist AI personas.
