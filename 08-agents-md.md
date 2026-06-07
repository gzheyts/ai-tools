# Section 8: AGENTS.md -- The Project Brain

`AGENTS.md` is a markdown file placed at the root of your repository that
provides every AI coding agent with essential project context. It is loaded
on **every single interaction**, so its content directly shapes the quality
of every response you receive.

## Why AGENTS.md Matters

Without it, an AI assistant will:
- Guess your build tool (Gradle? Maven? Ant?)
- Invent package names
- Ignore your coding conventions
- Use wrong Java version features
- Produce tests that don't match your testing framework

With a well-written `AGENTS.md`, the assistant knows your project as well
as a senior team member.

## The Specification

`AGENTS.md` follows the Linux Foundation standard adopted across 60,000+
repositories. It is recognized by Cursor, OpenCode, SourceCraft, Claude Code,
GitHub Copilot, and many others.

### Placement
```
your-project/
├── AGENTS.md           <-- project root (always loaded)
├── src/
│   └── main/
│       └── AGENTS.md   <-- subdirectory (loaded when working in this dir)
└── pom.xml
```

You can place additional `AGENTS.md` files in subdirectories to provide
context specific to that part of the codebase (e.g., one for `src/main/`,
another for `src/test/`).

### Loading Hierarchy
```
~/.config/<assistant>/AGENTS.md    (user-level, lowest priority)
./AGENTS.md                        (project root)
./src/main/AGENTS.md               (subdirectory, highest priority)
```

Subdirectory files extend (not replace) the root file.

## Structure of a Good AGENTS.md

A production-quality `AGENTS.md` has these sections:

### 1. Project Overview
What this project is, what language and framework it uses.

```markdown
## Project Overview

**my-service** -- Spring Boot 3.5 application for user management.

- **Language**: Java 21
- **Framework**: Spring Boot 3 with Spring Data JDBC
- **Database**: PostgreSQL (H2 for tests)
- **Build Tool**: Maven 3.9+
- **Package**: com.example.myservice
```

### 2. Build, Test & Lint Commands
Exact commands so the AI never guesses.

```markdown
## Build, Test & Lint Commands

### Build
\```bash
./mvnw clean package
./mvnw clean package -DskipTests
\```

### Test
\```bash
# All tests
./mvnw test

# Single class
./mvnw test -Dtest=PersonServiceTest

# Single method
./mvnw test -Dtest=PersonServiceTest#createPerson_validInput_returnsPerson

# With coverage
./mvnw test jacoco:report
\```

### Code Quality
\```bash
./mvnw versions:display-dependency-updates
./mvnw dependency:analyze
\```
```

### 3. Code Style Guidelines
Import order, formatting rules, naming conventions.

```markdown
## Code Style Guidelines

### Import Order
1. jakarta.*
2. lombok.*
3. org.springframework.*
4. com.example.* (project packages)
5. java.* / java.util.*
6. static imports (separated by blank line)

Never use wildcard imports.

### Naming
- Classes: PascalCase (PersonService)
- Implementations: {Interface}Impl (PersonServiceImpl)
- Tests: {ClassUnderTest}Test (PersonServiceTest)
- Methods: camelCase (createPerson)
- Constants: UPPER_SNAKE_CASE (MAX_RETRY_COUNT)
```

### 4. Architecture Rules
Layer boundaries and dependency rules.

```markdown
## Architecture

### Layered Architecture
1. **Controller** (@RestController): HTTP only, delegates to Service
2. **Service** (@Service interface + Impl): Business logic, @Transactional
3. **Repository** (@Repository): Data access via Spring Data JDBC
4. **Entity**: Domain models, never exposed via API
5. **Mapper**: Entity <-> DTO conversion via MapStruct

### Hard Rules
- Controllers inject Service interfaces only (NEVER Repositories directly)
- Services delegate all SQL to Repositories (NEVER execute raw SQL)
- All inter-layer data transfer uses DTOs (records)
- Return DTO records from all REST endpoints (NEVER entities)
```

### 5. Testing Patterns
How tests should be structured.

```markdown
## Testing

- JUnit 5 with Spring Boot Test
- Test naming: methodName_stateUnderTest_ExpectedBehavior
- Use ObjectMother pattern for test data
- Integration tests extend AbstractComponentTest
- BDD structure: given / when / then / verify
```

### 6. Common Patterns
Reusable code patterns so the AI produces consistent output.

```markdown
## Common Patterns

### Error Handling
- EntityNotFoundException for missing resources (404)
- ConflictException for state conflicts (409)
- ValidationException for business rule violations (422)
- Log errors with context: log.error("msg: id={}", id, ex)

### Optional Handling
Person person = repository.findById(id)
    .orElseThrow(() -> new EntityNotFoundException("Person not found", id));

### DTOs
- Always use Java records
- Apply @Valid on @RequestBody in controllers
- Use compact canonical constructors for validation
```

## Anti-Patterns to Avoid

| Anti-Pattern | Why It's Bad |
|---|---|
| Putting entire codebase docs in AGENTS.md | Wastes context tokens on every prompt |
| Omitting build commands | AI guesses wrong, wastes your time fixing |
| No architecture rules | AI puts SQL in controllers, returns entities |
| Too vague ("follow best practices") | AI interprets differently every time |
| Duplicating README.md content | AGENTS.md is for the AI, README is for humans |

## Prompting Tips for AGENTS.md

Applying research-backed prompt engineering principles (see
[Section 3: Prompting](03-prompting.md)) directly improves how your AI
assistant reads and follows `AGENTS.md`:

1. **Specify audience and role** (Principle 2, 16) -- Add a statement like
   "The AI assistant operates as a senior Java 21 engineer on this project"
   to the Project Overview. This primes the model's expertise level for
   every interaction.

2. **Use affirmative directives** (Principle 4) -- Write architecture rules
   as "do this" rather than "don't do that." Positive instructions are
   processed more reliably by LLMs.
   ```markdown
   # Less reliable
   - Controllers CANNOT inject Repositories

   # More reliable
   - Controllers inject Service interfaces only
   ```

3. **Use delimiters and structure** (Principle 8, 17) -- Separate sections
   with clear markdown headers. The model parses structured documents
   better than walls of text.

4. **State requirements explicitly** (Principle 9, 25) -- Use "MUST" and
   "NEVER" for hard rules that should not be violated. Vague phrasing like
   "prefer" or "try to" is treated as optional by the model.

## Real-World Example

See the complete `AGENTS.md` from a production Spring Boot service in
[templates/AGENTS.md](templates/AGENTS.md).

## Before/After: Vague vs Precise AGENTS.md

The difference between a vague and a precise `AGENTS.md` is the difference
between an AI that guesses and an AI that follows your standards.

**Bad AGENTS.md (vague):**
```markdown
We use Java and Spring Boot. Follow best practices. Write clean code.
```

This tells the AI almost nothing. It will pick its own Java version, its
own patterns, and a random testing style. You will spend more time fixing
the output than you saved by using AI.

**Good AGENTS.md (precise):**
```markdown
## Language & Framework
- Java 21 with records, sealed interfaces, and pattern matching
- Spring Boot 3.5 with Spring Data JDBC (not JPA)
- Constructor injection only; no field injection

## Build Commands
- Build: `mvn clean verify -T1C`
- Test single class: `mvn test -pl :module-name -Dtest=ClassName`
- Liquibase diff: `mvn liquibase:diff`

## Code Style
- DTOs are Java records; entities are mutable classes with private setters
- Use MapStruct for mapping (not manual or ModelMapper)
- Service methods return Optional<T> for single lookups, never null
```

Every line is actionable: the AI can follow it without ambiguity. Note how
"not JPA", "not manual or ModelMapper", and "never null" eliminate the most
common wrong choices.

---

## AGENTS.md Maintenance Strategy

An `AGENTS.md` that was accurate six months ago is probably wrong today.
Treat it as a living document.

### How to Keep AGENTS.md Current

1. **Review during sprint retrospectives.** Add a recurring agenda item:
   "Is AGENTS.md still accurate?" Takes 5 minutes when done regularly.

2. **Update when stack versions change.** Bumped Spring Boot from 3.4 to
   3.5? Update `AGENTS.md` the same day. Otherwise the AI generates code
   with the old API.

3. **Add new patterns as they emerge from code reviews.** If a code review
   comment says "we always do X," that rule belongs in `AGENTS.md`.

4. **Remove obsolete sections.** Dead frameworks, deprecated patterns, and
   removed modules create noise and confuse the model.

5. **Track evolution with git.** Use `git log --follow AGENTS.md` to see
   how the file changed over time and who changed it. This helps new team
   members understand why certain rules exist.

---

## Hierarchical AGENTS.md: When and How

A single root-level `AGENTS.md` works for small projects. As the codebase
grows, directory-level files provide scoped context that keeps the root
file lean.

### When to Use Directory-Level AGENTS.md

Use a subdirectory `AGENTS.md` when a part of the codebase has conventions
that differ from (or extend) the project root:

- **`src/test/AGENTS.md`** -- testing conventions, test data patterns,
  which base class to extend, H2 vs Testcontainers configuration.

- **`src/main/resources/db/AGENTS.md`** -- database migration rules:
  Liquibase vs Flyway naming conventions, changeset ID format, rollback
  requirements, how to handle data migrations.

- **Module-level AGENTS.md in a multi-module Maven project** -- each
  section (e.g., `order-service/`, `user-service/`) can have its own
  `AGENTS.md` with module-specific dependencies, API contracts, and
  domain terminology.

### Example Directory Structure

```
my-project/
├── AGENTS.md                          # Project-wide rules
├── order-service/
│   ├── AGENTS.md                      # Order domain terminology, aggregate rules
│   └── src/
│       ├── main/resources/db/
│       │   └── AGENTS.md              # Migration naming: YYYYMMDD_HHmmss_description
│       └── test/
│           └── AGENTS.md              # Use @OrderTestcontainers, not @SpringBootTest
└── user-service/
    ├── AGENTS.md                      # User domain, LDAP integration patterns
    └── src/
```

### Rules of Thumb

- **Root file**: Stack, build commands, company-wide style, architecture
  boundaries -- things that apply everywhere.
- **Subdirectory files**: Domain vocabulary, module-specific testing setup,
  database migration conventions -- things that apply only here.
- **Keep subdirectory files short** (30–60 lines). If one grows past 100
  lines, consider extracting reusable parts into a Skill instead.

---

## Quick-Reference Cheat Sheet

| Section | Purpose | Example (one line) |
|---------|---------|-------------------|
| Project Overview | Stack identity for the AI | `Java 21, Spring Boot 3.5, Spring Data JDBC, PostgreSQL` |
| Build Commands | Exact runnable commands | `./mvnw clean verify -T1C` |
| Test Commands | How to run tests at every granularity | `./mvnw test -Dtest=PersonServiceTest#create_valid_returns` |
| Code Style | Import order, naming, formatting | `Never use wildcard imports; imports ordered jakarta→spring→project→java` |
| Architecture | Layer boundaries and hard rules | `Controllers inject Service interfaces only; never Repository` |
| Testing Patterns | Framework, naming, data strategy | `JUnit 5, BDD given/when/then, ObjectMother for test data` |
| Common Patterns | Reusable code templates | `repository.findById(id).orElseThrow(() -> new EntityNotFoundException(...))` |
| Error Handling | Exception hierarchy and logging | `EntityNotFoundException→404, ConflictException→409, ValidationException→422` |
| AI Model Strategy | Which model for which task | `Default: Sonnet 4.5; thinking for debugging/architecture only` |
| Dependencies | Approved libraries and versions | `MapStruct 1.6, Liquibase 4.x, Testcontainers for integration tests` |

---

## Common AGENTS.md Anti-Patterns

Avoid these mistakes that reduce the effectiveness of your `AGENTS.md`:

### 1. Too Long (>200 Lines)

An `AGENTS.md` that exceeds 200 lines wastes context tokens on every
interaction. The model reads the entire file for every prompt, so every
unnecessary line costs money and dilutes attention. Extract reusable,
detailed patterns into Skills instead (see
[Section 9: Skills](09-skills.md)).

### 2. Too Generic ("Follow Clean Code")

Phrases like "follow best practices" or "write clean code" are
meaningless to an LLM. The model has read every coding book; it needs
to know **your** specific interpretation. Replace "clean code" with
concrete rules: "methods under 20 lines," "no more than 3 parameters,"
"extract private methods for each logical step."

### 3. Contradictory Rules

As `AGENTS.md` evolves, rules can contradict each other. For example,
one section says "use Lombok `@Builder`" while another says "use Java
records for all DTOs." Audit for consistency quarterly, or whenever a
team member reports that the AI is producing inconsistent output.

### 4. Implementation Details That Change Often

Pinning exact library patch versions (e.g., "use MapStruct 1.6.0.Beta2")
creates maintenance burden. Prefer stable abstractions: "use MapStruct 1.6+"
unless a specific version matters for compatibility.

### 5. Copy-Pasted from Another Project Without Customization

Every project is different. An `AGENTS.md` copied from a microservice
project won't work for a monolith. Review every line and ask: "Does this
apply to **this** project?" Remove anything that does not.

---

## Next Section

Proceed to [Section 9: Skills](09-skills.md) to learn how to build
reusable, modular capabilities.
