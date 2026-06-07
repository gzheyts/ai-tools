---
description: Review Java/Spring Boot code for architecture and quality issues
---

Review $ARGUMENTS for:

1. Architecture violations (controllers with business logic, entity exposure, layer bypass)
2. Java 21 idioms (records for DTOs, pattern matching, text blocks)
3. Code quality (import order, annotations, naming, null safety)
4. Security (input validation, SQL injection, sensitive data logging)

Follow conventions from AGENTS.md.

Output as: Critical > Warning > Suggestion.
Show problematic code and corrected version for each finding.
