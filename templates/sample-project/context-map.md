# Context Map: Which Files to Open Per Task

This document helps you prepare the optimal context for each task type.
Open the listed files before starting a conversation and close everything else.

## Feature Implementation

Open:
- `AGENTS.md`
- Target entity (e.g., `domain/Person.java`)
- Similar existing feature for reference (e.g., `controller/PersonController.java`)
- `dto/` package for DTO patterns

Close: test files, CI configs, Helm charts, unrelated entities.

Strategy: Use separate chats for implementation, testing, and review.

## Database Migration

Open:
- `AGENTS.md`
- `db/changelog/db.changelog-master.xml`
- Recent migration in `db/changelog/changes/` for pattern reference
- Target entity that will change

Close: controllers, services, tests, CI.

Strategy: Single focused chat. If MCP Postgres is configured, let the AI
inspect the current schema.

## Bug Fix / Debugging

Open:
- `AGENTS.md`
- The failing test file
- The code being tested

Paste into chat:
- Full error message and stack trace
- Recent `git log --oneline -5` if the bug may be a recent regression

Close: everything unrelated to the bug.

Strategy: Single focused chat. Provide error context upfront. Ask for
diagnosis before fix.

## Code Review

Open:
- `AGENTS.md`

Paste into chat or provide via git:
- The diff (`git diff main...feature-branch`)

Close: all source files (let the AI read what it needs from the diff).

Strategy: Use the `code-review` skill for consistent output format.

## Test Generation

Open:
- `AGENTS.md`
- The class to test (e.g., `service/impl/PersonServiceImpl.java`)
- An existing test for patterns (e.g., `PersonControllerTest.java`)

Close: unrelated source and test files.

Strategy: Use the `generate-tests` skill. Review generated tests for
real assertions (not just "it compiles").

## CI/CD Pipeline Fix

Open:
- `AGENTS.md`
- `.gitlab-ci.yml`
- Relevant Helm chart files (if deployment issue)

Paste into chat:
- CI job error output

Close: Java source files (not relevant for CI fixes).

Strategy: Single focused chat with the error output.
