# AGENTS.md -- Developer Guide for AI Coding Agents

## Project Overview

**demo-service** -- Spring Boot 3.5 REST API for managing persons and orders.

- **Language**: Java 21
- **Framework**: Spring Boot 3.5 with Spring Data JDBC
- **Database**: PostgreSQL 15 (H2 for tests)
- **Build Tool**: Maven 3.9+
- **Package**: com.example.demo

## Build, Test & Lint Commands

### Build
```bash
./mvnw clean package
./mvnw clean package -DskipTests
./mvnw clean install
```

### Test
```bash
./mvnw test
./mvnw test -Dtest=PersonControllerTest
./mvnw test -Dtest=PersonControllerTest#findById_existingPerson_returnsPerson
./mvnw test jacoco:report
```

### Code Quality
```bash
./mvnw versions:display-dependency-updates
./mvnw dependency:analyze
./mvnw liquibase:validate
```

## Code Style

### Import Order
1. jakarta.*
2. lombok.*
3. org.springframework.*
4. com.example.* (project packages)
5. java.* / java.util.*
6. static imports (separated by blank line)

No wildcard imports.

### Naming
- Classes: PascalCase (PersonService)
- Implementations: {Interface}Impl (PersonServiceImpl)
- Tests: {ClassUnderTest}Test (PersonServiceTest)
- Methods: camelCase (createPerson)
- Constants: UPPER_SNAKE_CASE (MAX_RETRY)
- Packages: lowercase singular (service, controller)

### Annotations
Order: Lombok -> Spring -> Validation -> Docs

```java
@Slf4j
@RequiredArgsConstructor
@Service
public class PersonServiceImpl implements PersonService { }
```

### DTOs
- Java records only
- @Valid on @RequestBody
- Compact canonical constructors for validation

## Architecture

### Layers
1. Controller (@RestController): HTTP only, delegates to Service
2. Service (interface + Impl): Business logic, @Transactional
3. Repository (@Repository): Spring Data JDBC
4. Entity: Domain model, never exposed via API
5. Mapper: MapStruct @Mapper(componentModel = "spring")

### Rules
- Controllers CANNOT inject Repositories
- Services CANNOT run raw SQL
- Inter-layer transfer uses DTOs (records)
- Entities never returned from endpoints

## Error Handling
- EntityNotFoundException -> 404
- ConflictException -> 409
- ValidationException -> 422
- Log with context: log.error("msg: id={}", id, ex)

## Testing
- JUnit 5 + Spring Boot Test
- Naming: methodName_stateUnderTest_ExpectedBehavior
- ObjectMother pattern for test data
- BDD: given / when / then

## Database
- PostgreSQL types (UUID, TIMESTAMPTZ, JSONB)
- Tables: snake_case plural (persons, orders)
- Columns: snake_case (first_name, created_at)
- Indexes: idx_{table}_{column}
- Liquibase XML with rollback and preconditions
- Changelogs: db/changelog/changes/YYYY-MM-DD-NNN-desc.xml

## Security Conventions

- All controller endpoints require authentication unless explicitly
  documented as public
- Use `@Valid` on all request body parameters
- DTOs must never contain password or secret fields
- Error responses use ProblemDetail (RFC 9457), never expose stack traces
- Log at WARN/ERROR level for security events, never log PII
- Dependencies must not have known CVEs (enforced by OWASP check in CI)
- Use parameterized queries only; never concatenate SQL strings
- No `@CrossOrigin("*")` in production code

## AI Assistant Configuration

- `.cursorignore` excludes secrets, prod configs, and MCP credentials
- MCP Postgres server configured with a read-only database user
- See `context-map.md` for which files to open per task type
- Full AGENTS template: [templates/AGENTS.md](../../AGENTS.md)
- Skills: code-review, generate-tests, db-migration, schema-review, ci-fix

---
**Last Updated**: 2026-04-10
