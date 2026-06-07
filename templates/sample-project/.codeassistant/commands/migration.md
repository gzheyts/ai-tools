---
description: Generate Liquibase migration changelog
argument-hint: <description-of-change>
---

Generate a Liquibase XML changelog with preconditions, rollback,
PostgreSQL types (Oracle comments), proper naming conventions.

File: src/main/resources/db/changelog/changes/YYYY-MM-DD-NNN-desc.xml
Warn if destructive. Remind to update master changelog.
