Review the currently open file (or the file I specify) for:

1. Architecture violations:
   - Business logic in controllers
   - Direct repository injection in controllers
   - Entities exposed in REST responses
   - Raw SQL in services

2. Java 21 best practices:
   - Use records for DTOs
   - Pattern matching switch
   - Text blocks for multi-line strings
   - Sealed classes where appropriate

3. Code quality:
   - Import order (jakarta, lombok, spring, project, java, static)
   - No wildcard imports
   - Annotation order (Lombok -> Spring -> Validation -> Docs)
   - Methods under 30 lines
   - Proper null safety

4. Security:
   - @Valid on @RequestBody
   - No SQL concatenation
   - Sensitive data not logged

Present findings grouped as: Critical, Warning, Suggestion.
Show the problematic line and a corrected version for each finding.
