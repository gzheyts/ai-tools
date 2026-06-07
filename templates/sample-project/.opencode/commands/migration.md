---
description: Generate a Liquibase migration changelog
---

Generate a Liquibase XML changelog for: $ARGUMENTS

Requirements:
- ID: today's date + NNN + description
- Include preconditions and rollback
- PostgreSQL types (Oracle alternatives in comments)
- File: src/main/resources/db/changelog/changes/

Naming: tables=snake_case plural, columns=snake_case,
indexes=idx_{table}_{col}, FKs=fk_{src}_{ref}.

Warn if destructive (DROP/RENAME).
