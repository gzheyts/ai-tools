# AGENTS.md -- Developer Guide for AI Coding Agents

## Project Overview

**my-service** -- Spring Boot application for [describe purpose].

- **Language**: Java 21
- **Framework**: Spring Boot 3.5 with Spring Data JDBC
- **Database**: PostgreSQL 15+ (H2 for tests)
- **Build Tool**: Maven 3.9+
- **Package**: com.example.myservice

## Build, Test & Lint Commands

### Build
```bash
# Clean build
./mvnw clean package

# Build without tests
./mvnw clean package -DskipTests

# Install to local repository
./mvnw clean install
```

### Test
```bash
# Run all tests
./mvnw test

# Run single test class
./mvnw test -Dtest=PersonServiceTest

# Run single test method
./mvnw test -Dtest=PersonServiceTest#createPerson_validInput_returnsPerson

# Run with coverage
./mvnw test jacoco:report
```

### Code Quality
```bash
# Check dependency updates
./mvnw versions:display-dependency-updates

# Analyze dependencies
./mvnw dependency:analyze

# Validate Liquibase changelogs
./mvnw liquibase:validate
```

## Code Style Guidelines

### Import Order
1. jakarta.*
2. lombok.*
3. org.springframework.*
4. com.example.* (project packages)
5. java.* / java.util.*
6. static imports (separated by blank line)

Never use wildcard imports.

### Formatting
- Indent: 4 spaces (not tabs)
- Line length: 120 characters max
- Encoding: UTF-8
- Braces: end of line for methods/blocks

### Naming Conventions
- Classes: PascalCase (PersonService, OrderController)
- Interfaces: PascalCase (PersonService)
- Implementations: {Interface}Impl (PersonServiceImpl)
- Methods: camelCase (createPerson, findById)
- Variables: camelCase (personDto, userId)
- Constants: UPPER_SNAKE_CASE (MAX_RETRY_COUNT)
- Test classes: {ClassUnderTest}Test (PersonServiceTest)
- Packages: lowercase, singular (service, repository, controller)

### Annotation Ordering
```java
// Class-level: Lombok -> Spring stereotype -> Config -> Docs
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PersonServiceImpl implements PersonService {

    // Method-level: Transaction -> Override -> Custom
    @Transactional
    @Override
    public PersonResponse create(@NonNull @Valid CreatePersonRequest request) {
        // ...
    }
}
```

### DTOs and Records
- DTOs MUST be Java records
- Use compact canonical constructors for validation
- Use @Valid on @RequestBody in controllers

```java
public record CreatePersonRequest(
    @NotNull String firstName,
    @NotNull String lastName,
    @NotNull Sex sex
) {
    public CreatePersonRequest {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("firstName is required");
        }
    }
}
```

### Java 21 Requirements
- DTOs: Java records (not Lombok @Data classes)
- Switch: pattern matching switch expressions
- Hierarchies: sealed classes/interfaces for domain events
- Strings: text blocks for multi-line SQL, JSON templates
- Null safety: @NonNull/@Nullable from Lombok/Jakarta, Optional for return types

## Architecture

### Layered Architecture
1. **Controller** (@RestController): HTTP handling only
   - Uses DTOs for request/response
   - Delegates business logic to Service
   - Applies @Valid on @RequestBody
   - Uses OpenAPI annotations (@Tag, @Operation)

2. **Service** (interface + @Service Impl): Business logic
   - All implementations in {Service}Impl classes
   - Uses @Transactional for write operations
   - Receives and returns DTOs, not entities
   - Constructor injection via @RequiredArgsConstructor

3. **Repository** (@Repository): Data access
   - Spring Data JDBC (CrudRepository)
   - Method naming conventions for queries
   - @Query for complex queries with named parameters

4. **Entity**: Domain models
   - NEVER exposed through REST API
   - @Table annotation (Spring Data JDBC)
   - @Id for primary key

5. **Mapper**: Entity <-> DTO conversion
   - MapStruct: @Mapper(componentModel = "spring")
   - Declarative mapping only

### Hard Rules
- Controllers CANNOT inject Repositories directly
- Services CANNOT execute raw SQL (use Repositories)
- Data transfer between layers MUST use DTOs (records)
- Entities are never returned from REST endpoints

## Error Handling
- EntityNotFoundException: resource not found (404)
- ConflictException: state conflicts (409)
- ValidationException: business rule violations (422)
- Log errors with context: log.error("msg: id={}", id, ex)
- Never swallow exceptions silently

## Logging
- debug: Method entry with parameters
- info: Significant business events
- warn: Recoverable issues
- error: Failures requiring attention

```java
@Slf4j
public class PersonServiceImpl {
    public PersonResponse create(CreatePersonRequest request) {
        log.debug("create: request={}", request);
        // ... logic
        log.info("Person created: id={}", person.getId());
        return response;
    }
}
```

## Testing

### Conventions
- JUnit 5 with Spring Boot Test
- Test naming: methodName_stateUnderTest_ExpectedBehavior
- Extend AbstractComponentTest for integration tests
- Use ObjectMother pattern for test data builders
- BDD structure: given / when / then / verify
- Use @Transactional on tests for auto-rollback

### Example
```java
class PersonServiceTest extends AbstractComponentTest {

    @Test
    void createPerson_validInput_returnsPerson() {
        // given
        var request = ObjectMother.createPersonRequestBuilder().build();

        // when
        var response = personService.create(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.firstName()).isEqualTo(request.firstName());
    }
}
```

## Database

### Schema Conventions
- Tables: snake_case, plural (persons, orders)
- Columns: snake_case (first_name, created_at)
- Primary key: id (UUID)
- Foreign keys: {table_singular}_id (person_id)
- Timestamps: created_at, updated_at (TIMESTAMPTZ)
- Indexes: idx_{table}_{column(s)}

### Liquibase
- Format: XML
- Master: db/changelog/db.changelog-master.xml
- Files: db/changelog/changes/YYYY-MM-DD-NNN-description.xml
- Always include rollback
- Use preconditions for safety

## GitLab CI

### Pipeline
- Uses shared templates via include:
- Stages: build -> test -> docker -> deploy
- Never hardcode secrets
- Use rules: (not only:/except:)

## Kubernetes / Helm

### Chart Structure
- Directory: .helm/
- Always include liveness/readiness probes (Spring Actuator)
- Set resource requests AND limits
- Use securityContext (non-root)

---

**Last Updated**: YYYY-MM-DD
**Maintainer**: Team Name (team@example.com)
