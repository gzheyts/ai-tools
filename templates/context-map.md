# Context Map: Which Files to Open Per Task

Customize this for your project. Open the listed files before starting an
AI conversation and close everything else to keep context focused.

## Feature Implementation

Open:
- `AGENTS.md`
- Target entity / model file
- Similar existing feature for reference
- DTO / mapper package for patterns

Close: test files, CI configs, Helm charts, unrelated modules.

Strategy: Use separate chats for implementation, testing, and review.

## Database Migration

Open:
- `AGENTS.md`
- `db/changelog/db.changelog-master.xml` (or equivalent)
- Recent migration for pattern reference
- Target entity that will change

Close: controllers, services, tests, CI configs.

Strategy: Single focused chat. Use MCP Postgres if available.

## Bug Fix / Debugging

Open:
- `AGENTS.md`
- The failing test or error location
- The code being tested

Paste into chat:
- Full error message and stack trace
- Recent git log if regression suspected

Close: everything unrelated.

Strategy: Single focused chat. Ask for diagnosis before fix.

## Code Review

Open:
- `AGENTS.md`

Paste into chat:
- The diff (or provide via git reference)

Close: all source files.

Strategy: Use the code-review skill for consistent output.

## Test Generation

Open:
- `AGENTS.md`
- The class to test
- An existing test for patterns

Close: unrelated source and test files.

Strategy: Use the generate-tests skill.

## CI/CD Pipeline Fix

Open:
- `AGENTS.md`
- `.gitlab-ci.yml` (or equivalent CI config)
- Relevant Helm/K8s files (if deployment issue)

Paste into chat:
- CI job error output

Close: Java source files.

Strategy: Single focused chat with error output.
