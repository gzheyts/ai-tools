# Section 11: Agents & Subagents -- Orchestrating Specialists

An **agent** is a specialist AI persona you explicitly summon to handle
complex, multi-step tasks. While a skill is a "verb" (one action) and a
command is a "shortcut" (one prompt), an agent is a "person" -- a specialist
with goals, tools, and the ability to plan and reason.

## Skills vs. Commands vs. Agents

```text
┌──────────────────────────────────────────────────────┐
│                      Complexity                       │
│                                                      │
│  Command ──────> Skill ──────> Agent ──────> Subagent│
│  "/review"       auto-trigger   specialist    team   │
│  one prompt      one task       multi-step    multi- │
│                                 reasoning     agent  │
└──────────────────────────────────────────────────────┘
```

| Feature       | Command          | Skill            | Agent             |
|---------------|------------------|------------------|-------------------|
| Trigger       | User types `/`   | AI auto-detects  | User summons      |
| Complexity    | Single prompt    | Single task      | Multi-step plan   |
| Reasoning     | None             | Follows recipe   | Plans and adapts  |
| Context       | Current file     | Task-specific    | Fresh 200K window |
| Tools         | Chat only        | Read/search      | Read/write/shell  |

## What Are Subagents?

Subagents are specialized AI instances that an **orchestrator** (main agent)
delegates work to. Each subagent:

- Gets its own fresh context window (no context pollution)
- Focuses on a single phase of work
- Returns results to the orchestrator

### Why Subagents?

Single-agent problems:
1. **Context overflow**: After 50K+ tokens, the AI forgets earlier context
2. **Attention dilution**: Mixing research, planning, and coding degrades quality
3. **No specialization**: One agent can't be expert at everything simultaneously

Subagent benefits:
- **90% performance improvement** on complex tasks (Anthropic research)
- **60% token reduction** compared to monolithic prompts
- **Parallel execution** -- multiple subagents work simultaneously

## Architecture: The Orchestrator Pattern

```text
┌─────────────────────────────────────────────────┐
│                  Orchestrator                    │
│           (main agent, coordinates)             │
│                                                 │
│  ┌──────────┐ ┌──────────┐ ┌───────────────┐   │
│  │ Research  │ │ Planning │ │ Implementation│   │
│  │ Subagent  │ │ Subagent │ │   Subagent    │   │
│  │           │ │          │ │               │   │
│  │ - Read    │ │ - Analyze│ │ - Write code  │   │
│  │ - Search  │ │ - Design │ │ - Run tests   │   │
│  │ - Analyze │ │ - Plan   │ │ - Fix errors  │   │
│  └──────────┘ └──────────┘ └───────────────┘   │
│        │            │              │            │
│        ▼            ▼              ▼            │
│  ┌──────────────────────────────────────────┐   │
│  │           Validation Subagent            │   │
│  │    - Run tests, verify, code review      │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

## Built-In Subagent Types

### Cursor

Cursor provides three built-in subagent types via the Task tool:

| Type             | Mode      | Best For                           |
|------------------|-----------|------------------------------------|
| `explore`        | Read-only | Searching code, finding patterns   |
| `generalPurpose` | Read/Write| Complex multi-step implementations |
| `shell`          | Execute   | Running commands, git operations   |

**Example -- parallel exploration:**
You can launch multiple explore subagents simultaneously to research
different parts of the codebase:

```
"Launch two subagents in parallel:
 1. Find all REST controllers and their endpoints
 2. Find all database migrations and their current state"
```

### OpenCode

OpenCode supports custom agent definitions in its configuration:

```json
{
  "agents": {
    "reviewer": {
      "description": "Code review specialist",
      "instructions": "You are a senior Java developer reviewing code...",
      "model": "anthropic/claude-sonnet-4-20250514"
    },
    "tester": {
      "description": "Test generation specialist",
      "instructions": "You are a testing expert using JUnit 5...",
      "model": "anthropic/claude-sonnet-4-20250514"
    }
  }
}
```

### SourceCraft

SourceCraft uses agent roles defined in project configuration
or via IDE settings. Consult your SourceCraft documentation for
the latest configuration format.

## Designing Subagent Workflows for Java Projects

### Workflow 1: Feature Implementation

When implementing a new feature, break it into phases:

```text
Phase 1: Research (explore subagent, read-only)
  └─ Analyze existing code patterns
  └─ Find similar implementations
  └─ Check AGENTS.md conventions

Phase 2: Plan (generalPurpose subagent, read-only)
  └─ Design the solution
  └─ List files to create/modify
  └─ Identify potential issues

Phase 3: Implement (generalPurpose subagent, read/write)
  └─ Create/modify files
  └─ Follow patterns from Phase 1
  └─ Apply conventions from AGENTS.md

Phase 4: Validate (shell subagent)
  └─ Run ./mvnw test
  └─ Check for compilation errors
  └─ Verify code style
```

### Workflow 2: Code Review Pipeline

```text
Subagent 1: Architecture Review (parallel)
  └─ Check layer boundaries
  └─ Verify DTO usage
  └─ Check dependency directions

Subagent 2: Security Review (parallel)
  └─ Input validation
  └─ SQL injection
  └─ Sensitive data exposure

Subagent 3: Performance Review (parallel)
  └─ N+1 queries
  └─ Unnecessary object creation
  └─ Stream vs loop usage

Orchestrator: Consolidate findings
  └─ Merge all findings
  └─ Deduplicate
  └─ Prioritize by severity
```

### Workflow 3: Database Migration

```text
Subagent 1: Schema Analysis (explore)
  └─ Current schema state
  └─ Existing migrations
  └─ Index analysis

Subagent 2: Migration Generation (generalPurpose)
  └─ Generate Liquibase changelog
  └─ Include rollback
  └─ Handle PostgreSQL + Oracle

Subagent 3: Validation (shell)
  └─ Run ./mvnw liquibase:validate
  └─ Check for conflicts with existing changelogs
```

## Best Practices

### 1. Use Read-Only Subagents First
Always research before writing. Explore subagents are cheaper and faster.

### 2. Isolate Write Operations
Only one subagent should write to a file at a time.
Parallel writes to different files are safe.
Parallel writes to the same file will cause conflicts.

### 3. Pass Context Explicitly
Subagents don't share context. If Subagent 2 needs results from
Subagent 1, the orchestrator must pass that data explicitly.

### 4. Choose the Right Model
- Complex reasoning (architecture, planning): Use the most capable model
- Simple tasks (search, formatting): Use the fastest model
- Cost optimization: Mix models based on task complexity

### 5. Validate After Every Write Phase
Always run tests and linters after implementation subagents finish.
Don't assume code compiles or tests pass.

## When NOT to Use Subagents

- Simple, single-file changes (use a command instead)
- Quick questions about the codebase (use the main agent)
- Tasks that require seeing all context at once (use the main agent)

Subagents shine when the task is complex, multi-step, and benefits
from specialization or parallelization.

## Orchestrator Template with Error Handling

Below is a complete orchestrator pattern for a "Feature Implementation" workflow.
Each phase delegates to a specialized subagent, includes explicit error-handling
gates, and passes context forward through the orchestrator.

```
Phase 1: Analysis (explore subagent, read-only)
  - Read the user story / requirements
  - Identify affected files and layers
  - If analysis fails: report what's missing and stop

Phase 2: Implementation (generalPurpose subagent, read/write)
  - Generate entity, DTO, mapper, repository, service, controller
  - Follow AGENTS.md patterns
  - If build fails: fix compilation errors (max 3 attempts)

Phase 3: Testing (generalPurpose subagent, read/write)
  - Generate unit tests for service layer
  - Generate integration test for controller
  - Run tests, fix failures (max 3 attempts)

Phase 4: Review (explore subagent, read-only)
  - Review all changes against AGENTS.md conventions
  - Check for security issues, null safety, resource leaks
  - Report findings without modifying code
```

### Prompt Templates per Phase

**Phase 1 -- Analysis prompt:**
```
Analyze the following user story and identify what needs to change:

User story: "{paste user story here}"

Instructions:
1. List every file that must be created or modified (entity, DTO,
   mapper, repository, service, controller, test).
2. For each file, describe the change needed in one sentence.
3. Identify any existing patterns in the codebase that the new code
   should follow (check AGENTS.md).
4. Flag anything unclear or missing from the user story.

If you cannot identify the affected layers, stop and list the
questions that need answering before implementation can begin.
```

**Phase 2 -- Implementation prompt:**
```
Implement the feature based on the analysis below.

Analysis from Phase 1:
---
{paste Phase 1 output here}
---

Rules:
- Follow AGENTS.md conventions exactly.
- DTOs must be Java records.
- Annotation order: Lombok → Spring stereotype → Config.
- Service layer: interface + implementation.
- Include @Transactional on write operations.
- If the build fails, read the error, fix it, and retry (up to
  3 attempts). After 3 failed attempts, report the remaining
  errors and stop.
```

**Phase 3 -- Testing prompt:**
```
Generate tests for the code created in Phase 2.

Files created/modified:
---
{paste list of files from Phase 2}
---

Testing requirements:
- Unit tests for the service layer using Mockito.
- Integration test for the controller using @WebMvcTest.
- Test naming: methodName_stateUnderTest_ExpectedBehavior
- Structure: Given/When/Then with section comments.
- AssertJ for assertions, not JUnit assertions.
- Run ./mvnw test after generating tests.
- If tests fail, fix and rerun (up to 3 attempts).
- After 3 failed attempts, report remaining failures and stop.
```

**Phase 4 -- Review prompt:**
```
Review all files changed in this feature for quality and security.
Do NOT modify any code. Report findings only.

Changed files:
---
{paste list of changed files}
---

Review checklist:
1. AGENTS.md compliance: annotation order, naming, layer boundaries.
2. Security: input validation, SQL injection, sensitive data exposure.
3. Null safety: Optional usage, @NonNull annotations, null checks.
4. Resource leaks: streams, connections, closeable resources.
5. Code quality: unused imports, dead code, Lombok misuse.

Output format: table with columns [File | Issue | Severity | Suggestion].
```

## OpenCode JSON Agent Configuration

OpenCode allows you to define specialized agents in your project's
configuration file. Each agent gets its own system prompt, model
selection, and tool permissions.

### Reviewer Agent (read-only)

```json
{
  "agents": {
    "reviewer": {
      "description": "Senior Java code reviewer (read-only)",
      "model": "anthropic/claude-sonnet-4-20250514",
      "instructions": "You are a senior Java developer performing code review. You NEVER modify files -- only read and report findings. Check for: AGENTS.md compliance, OWASP Top 10, null safety, resource leaks, unused imports, incorrect annotation ordering. Output a markdown table with columns: File | Line | Issue | Severity | Suggestion.",
      "tools": ["read", "search", "glob", "grep"],
      "maxTokens": 16384
    }
  }
}
```

### Migrator Agent (database specialist)

```json
{
  "agents": {
    "migrator": {
      "description": "Database migration specialist (PostgreSQL + Oracle)",
      "model": "anthropic/claude-sonnet-4-20250514",
      "instructions": "You are a DBA specializing in Liquibase migrations for PostgreSQL and Oracle. Generate changelogs in YAML format. Always include rollback blocks. Use database-agnostic types where possible (VARCHAR, BIGINT). When types differ between PostgreSQL and Oracle, use Liquibase's dbms precondition. Follow the existing changelog naming convention: YYYY-MM-DD-NN-description.yaml. Validate with ./mvnw liquibase:validate after generation.",
      "tools": ["read", "write", "search", "shell"],
      "maxTokens": 8192
    }
  }
}
```

### Tester Agent (test generation)

```json
{
  "agents": {
    "tester": {
      "description": "JUnit 5 test generation specialist",
      "model": "anthropic/claude-sonnet-4-20250514",
      "instructions": "You are a testing expert using JUnit 5, Mockito, and AssertJ. Generate tests following these conventions: test naming methodName_stateUnderTest_ExpectedBehavior, Given/When/Then structure with section comments, @ExtendWith(MockitoExtension.class) for unit tests, @SpringBootTest with @Testcontainers for integration tests, @WebMvcTest for controller slice tests. Use ObjectMother pattern for test data. Never hardcode UUIDs or timestamps. Target 100% coverage on service business logic.",
      "tools": ["read", "write", "search", "shell"],
      "maxTokens": 16384
    }
  }
}
```

## When NOT to Use Subagents

Subagents add orchestration overhead. Avoid them in these situations:

### Anti-pattern 1: Simple Single-File Changes

If the task is editing one file (renaming a method, adding a field, fixing
a typo), the overhead of spawning a subagent exceeds the benefit. Use a
direct prompt to the main agent instead.

```
❌ Spawn subagent to add a @NotNull annotation
✅ Just ask: "Add @NotNull to the firstName parameter in CreatePersonRequest"
```

### Anti-pattern 2: Tasks Requiring Shared State Between Steps

Subagent contexts are isolated. If Step 2 needs to reference a variable
computed in Step 1, the orchestrator must serialize and pass that state
explicitly. When the shared state is complex (a partial AST, a large
in-memory map), a single agent with full context is more efficient.

```
❌ Subagent 1 builds a dependency graph → Subagent 2 uses it to refactor
✅ Single agent: "Analyze the dependency graph and refactor based on it"
```

### Anti-pattern 3: Main Agent Already Has Full Context

If you've been iterating with the main agent for 20 messages and it
already understands the codebase, architecture, and your intent, spawning
a subagent that starts from scratch discards all of that context.

```
❌ After 20 messages of discussion: "Launch a subagent to implement what we discussed"
✅ Continue in the same conversation: "Now implement it based on our discussion"
```

### Anti-pattern 4: Quick Questions Without Tool Usage

If the question can be answered from the AI's training data or a single
file read, a subagent is unnecessary overhead.

```
❌ Spawn subagent to answer "What does @Transactional(readOnly=true) do?"
✅ Just ask the main agent directly
```

## Quick-Reference Cheat Sheet

| Pattern                  | Subagent Type     | Use Case                                | Context Sharing                          |
|--------------------------|-------------------|-----------------------------------------|------------------------------------------|
| Codebase exploration     | `explore`         | Find patterns, analyze architecture     | Returns summary text to orchestrator     |
| Feature implementation   | `generalPurpose`  | Multi-layer code generation             | Receives analysis from explore phase     |
| Test generation          | `generalPurpose`  | Unit + integration tests                | Receives file list from implementation   |
| Build & validation       | `shell`           | Compile, run tests, lint                | Receives file paths to validate          |
| Code review              | `explore`         | Read-only quality/security review       | Receives list of changed files           |
| Parallel reviews         | `explore` × N     | Architecture + Security + Performance   | Each receives same file list, no sharing |
| Database migration       | `generalPurpose`  | Generate Liquibase changelogs           | Receives schema analysis from explore    |
| Bulk refactoring         | `generalPurpose`  | Apply same change across many files     | Receives pattern + file list             |

**Decision rule:** If the task touches ≤ 2 files and requires no build
verification, skip subagents. If the task touches ≥ 3 layers or benefits
from parallel work, use subagents.

## Next Section

Proceed to [Section 13: Java Production Stack](13-java-production-stack.md) to learn
how to apply these patterns to Java 21, Spring Boot, database, and DevOps workflows.
