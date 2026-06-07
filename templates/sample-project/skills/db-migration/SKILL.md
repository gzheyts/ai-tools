---
name: db-migration
description: >
  Generate Liquibase database migration changelogs in XML format.
  Use when asked to create migrations, add columns, create tables,
  add indexes, modify schema, or generate database changes.
  Handles PostgreSQL and Oracle syntax differences.
---

## Instructions

### Step 1: Understand the Change
1. Identify what schema change is needed.
2. Check existing changelogs in db/changelog/changes/ for context.
3. Determine if this is additive or destructive.

### Step 2: Generate Changelog

#### File Naming
`YYYY-MM-DD-NNN-description.xml`
Example: `2026-04-04-001-add-email-to-persons.xml`

#### File Location
`src/main/resources/db/changelog/changes/`

#### Template
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="{date-nnn-description}" author="{developer}">
        <preConditions onFail="MARK_RAN">
            <!-- Safety check -->
        </preConditions>

        <!-- Change goes here -->

        <rollback>
            <!-- Undo the change -->
        </rollback>
    </changeSet>
</databaseChangeLog>
```

### Step 3: Type Mapping
Use PostgreSQL types by default. Add Oracle alternatives in XML comments.

| Concept   | PostgreSQL     | Oracle                     |
|-----------|----------------|----------------------------|
| ID        | UUID           | RAW(16)                    |
| String    | VARCHAR(n)     | VARCHAR2(n)                |
| Long text | TEXT           | CLOB                       |
| Boolean   | BOOLEAN        | NUMBER(1)                  |
| Timestamp | TIMESTAMPTZ    | TIMESTAMP WITH TIME ZONE   |
| JSON      | JSONB          | CLOB (+ IS JSON check)     |
| Money     | NUMERIC(19,4)  | NUMBER(19,4)               |

### Safety Rules
- Always include rollback
- Always include preconditions
- One logical change per changeset
- Never modify existing changeset IDs
- For destructive changes (DROP, RENAME):
  1. Warn the user about data loss risk
  2. Include preconditions
  3. Suggest multi-step migration if safer

### Step 4: Update Master Changelog
Remind the user to add an include to `db.changelog-master.xml`:
```xml
<include file="db/changelog/changes/YYYY-MM-DD-NNN-description.xml"/>
```
