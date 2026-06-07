Generate a Liquibase migration changelog for the schema change I describe.

Requirements:
- XML format
- Changeset ID: YYYY-MM-DD-NNN-description (use today's date)
- Author: "developer" (I will update)
- Include preconditions to prevent double-execution
- Include rollback statements
- Use PostgreSQL types (add Oracle equivalents in XML comments)
- Place file in: src/main/resources/db/changelog/changes/
- Remind me to add include to db.changelog-master.xml

Type mappings:
- ID: UUID (Oracle: RAW(16))
- String: VARCHAR(n) (Oracle: VARCHAR2(n))
- Long text: TEXT (Oracle: CLOB)
- Boolean: BOOLEAN (Oracle: NUMBER(1))
- Timestamp: TIMESTAMPTZ (Oracle: TIMESTAMP WITH TIME ZONE)
- JSON: JSONB (Oracle: CLOB)
- Money: NUMERIC(19,4) (Oracle: NUMBER(19,4))

Naming conventions:
- Tables: snake_case plural (persons, orders)
- Columns: snake_case (first_name, created_at)
- Indexes: idx_{table}_{column}
- Foreign keys: fk_{source}_{referenced}
- Unique: uq_{table}_{column}

If the change is destructive (DROP, RENAME), warn me first and suggest
a safer approach.
