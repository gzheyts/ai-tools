# Section 16: Java Production Stack — Spring Boot, Database & DevOps

Teach AI assistants your **Java 21 / Spring Boot 3.5** patterns, **PostgreSQL /
Oracle / Liquibase** conventions, and **GitLab CI / Kubernetes** infrastructure —
the full path from application code to production deployment.

| Part | Topic | Focus |
|------|-------|-------|
| [Java & Spring Boot](#java-21-and-spring-boot) | Records, layers, Maven, testing | Stop pre-Java-16 defaults |
| [Database](#database--postgresql-oracle--liquibase) | Schema, Liquibase, query tuning | Prevent unsafe migrations |
| [DevOps](#devops--gitlab-ci--kubernetes) | CI pipelines, Helm, K8s debugging | Catch infrastructure errors early |

---

## Java 21+ and Spring Boot

### Why Java Configuration Matters

Left unconfigured, AI assistants default to the Java they were most
heavily trained on — which is overwhelmingly **pre-Java 16 code**.
That means Lombok `@Data` instead of records, raw `Thread` instead
of virtual threads, `instanceof + cast` chains instead of pattern
matching, and Hibernate/JPA instead of Spring Data JDBC.

This has a concrete cost. Every suggestion you reject and retype is
a half-minute of friction. Multiplied across a team of 10 developers
doing 30 AI interactions a day, that is **hours of throwaway review
per sprint**. The configuration work in this section is a one-time
investment that pays back every single session.

There is a second, less obvious cost: **inconsistency**. An AI that
does not know your layer rules will happily inject a repository into
a controller, return `null` instead of `Optional`, or add a `@Transactional`
annotation in the wrong layer. Those suggestions are not just wrong —
they are subtly wrong, the kind of thing that survives code review and
becomes a support incident later.

The goal of this section is to eliminate that entire class of problem by
front-loading your project's patterns into `AGENTS.md`, skills, and commands.

## Java 21+ Features the AI Must Know

Modern Java has changed significantly. Without explicit guidance, AI
assistants often generate Java 8-style code. Your configuration should
enforce these patterns:

### Records for DTOs
```java
// AI should generate this (Java 16+)
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

// NOT this (pre-Java 16)
@Data
@AllArgsConstructor
public class CreatePersonRequest {
    private String firstName;
    private String lastName;
    private Sex sex;
}
```

### Sealed Classes for Domain Hierarchies
```java
public sealed interface PersonEvent
    permits PersonCreated, PersonUpdated, PersonDeleted {

    UUID personId();
    Instant occurredAt();
}

public record PersonCreated(UUID personId, Instant occurredAt, String name)
    implements PersonEvent {}
```

### Pattern Matching
```java
// AI should generate this (Java 21)
return switch (status) {
    case ACTIVE -> processActive(person);
    case INACTIVE -> processInactive(person);
    case BLOCKED -> throw new IllegalStateException("Blocked persons cannot be processed");
};

// NOT this
if (status == Status.ACTIVE) {
    return processActive(person);
} else if (status == Status.INACTIVE) {
    return processInactive(person);
} else {
    throw new IllegalStateException("...");
}
```

### Virtual Threads (Project Loom)

Virtual threads are **GA in Java 21** (JEP 444). No special flags needed.

```java
@Bean
public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
    return protocolHandler -> {
        protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    };
}
```

> **Note — Structured Concurrency (`StructuredTaskScope`) status by JDK version:**
>
> | JDK | Status | Flag required |
> |-----|--------|---------------|
> | 21  | Preview (JEP 453) | `--enable-preview` |
> | 22  | Second preview (JEP 462) | `--enable-preview` |
> | 23  | Third preview (JEP 480) | `--enable-preview` |
> | 24+ | GA (JEP 505) | None |
>
> The `StructuredTaskScope` examples in this section use the GA API shape.
> If your team is on JDK 21–23, add `--enable-preview` to your Maven
> Surefire and compiler plugin configuration, or use
> `Executors.newVirtualThreadPerTaskExecutor()` with a plain
> `CompletableFuture` chain as the stable alternative.

### Text Blocks for Multi-line Strings
```java
String query = """
    SELECT p.id, p.first_name, p.last_name
    FROM person p
    WHERE p.status = :status
    ORDER BY p.last_name
    """;
```

## Spring Boot 3.5+ Patterns

### Layered Architecture Configuration

Document your exact layer boundaries in `AGENTS.md`:

```markdown
## Architecture Layers

### Controller Layer
- Annotation: @RestController
- Responsibilities: HTTP handling, request validation, response mapping
- MUST use DTOs (records) for request/response
- MUST NOT contain business logic
- MUST NOT inject Repository beans directly
- MUST use OpenAPI annotations (@Tag, @Operation, @ApiResponse)

### Service Layer
- Interface + Impl pattern: PersonService / PersonServiceImpl
- Annotation: @Service on implementation
- MUST use @Transactional for write operations
- MUST receive and return DTOs, not entities
- MUST use constructor injection via @RequiredArgsConstructor

### Repository Layer
- Annotation: @Repository
- Extends CrudRepository or uses Spring Data JDBC
- Named queries follow Spring conventions: findByEmail, findAllByStatus
- Complex queries use @Query with named parameters

### Entity Layer
- Plain classes with @Table annotation (Spring Data JDBC)
- NEVER exposed through REST API
- Use @Id for primary key
- Relationships via AggregateReference (Spring Data JDBC)

### Mapper Layer
- MapStruct: @Mapper(componentModel = "spring")
- Declarative mapping only (no procedural logic)
- Separate mappers per entity/aggregate
```

### Maven Configuration Guidance

Include your Maven conventions in `AGENTS.md`:

```markdown
## Maven

### Plugin Versions
- Spring Boot Maven Plugin: inherited from parent
- MapStruct: 1.5.5.Final
- Lombok: inherited from Spring Boot BOM
- JaCoCo: 0.8.12

### Dependency Rules
- NEVER add dependency versions if managed by Spring Boot BOM
- Use spring-boot-starter-* where available
- Test dependencies: scope=test only
- Lombok: scope=provided, annotationProcessorPaths configured

### Build Profiles
- default: full build with tests
- fast: -DskipTests -Dmaven.javadoc.skip=true
- ci: includes JaCoCo coverage + dependency-check
```

### Annotation Ordering Standard

**Why it matters:** Without an explicit ordering standard, AI assistants
produce inconsistent annotation order across files in the same project.
That is pure noise in code review — the reviewer has to determine whether
`@Transactional` appearing before `@Service` is intentional. Enforcing
a single order in `AGENTS.md` eliminates that class of comment entirely.

Convention: **Lombok → Spring stereotype → Spring configuration → Custom**

```java
// Class-level: Lombok -> Spring stereotype -> Config
@Slf4j                          // Lombok
@RequiredArgsConstructor        // Lombok
@Service                        // Spring stereotype
@Transactional(readOnly = true) // Spring config
public class PersonServiceImpl implements PersonService {

    // Method-level: Transaction -> Override -> Custom
    @Transactional              // Spring
    @Override                   // Java
    public PersonResponse create(@NonNull @Valid CreatePersonRequest request) {
        log.debug("create: request={}", request);
        // ...
    }
}
```

### Testing Standards

```markdown
## Testing Conventions

### Test Class Structure
- Extend AbstractComponentTest for integration tests
- Use @Nested classes to group tests by method
- Each @Nested class tests one public method

### Naming
- Test class: {ClassName}Test (e.g., PersonServiceTest)
- Test method: methodName_stateUnderTest_ExpectedBehavior

### Data
- Use ObjectMother pattern for builders
- Never hardcode UUIDs or timestamps
- Use @Sql or Liquibase for database state setup

### Assertions
- Use AssertJ for fluent assertions
- Use MockMvc for controller tests
- Use @Transactional on tests to auto-rollback

### Coverage
- Minimum 80% line coverage
- 100% coverage on service layer business logic
- Controller tests must verify HTTP status codes and response structure
```

## Putting It All Together: AGENTS.md for Java Projects

A complete `AGENTS.md` for a Java project should include:

1. Project metadata (Java version, Spring Boot version, package)
2. Build commands (clean, test, coverage, single test)
3. Architecture layers with hard rules
4. Code style (imports, formatting, naming)
5. Annotation ordering
6. DTO strategy (records, validation)
7. Error handling patterns
8. Logging conventions
9. Testing patterns (naming, data, assertions)
10. Maven dependency rules

See [templates/AGENTS.md](templates/AGENTS.md) for a complete example.

## Skills for Java Development

Create these skills for your project:

| Skill Name        | Purpose                                    |
|-------------------|--------------------------------------------|
| `code-review`     | Review against project conventions         |
| `generate-tests`  | Create JUnit 5 tests with your patterns    |
| `spring-endpoint` | Scaffold full endpoint (Controller -> Test)|
| `refactor-java21` | Modernize code to Java 21 idioms           |
| `mapstruct-mapper`| Generate MapStruct mappers for entities    |

## Prompting Tips for Java/Spring Tasks

Applying prompt engineering principles (see
[Section 3: Prompting](03-prompting.md)) is especially important for
multi-layer Java features where the AI must produce coherent code
across controllers, services, repositories, and entities:

1. **Decompose multi-layer features** (Principle 3) -- Instead of
   "create the Order feature," break it into sequential prompts:
   schema first, then entities, then repository, then service, then
   controller. Each prompt builds on the previous output and stays
   within a focused scope.

2. **Use "think step by step" for complex logic** (Principle 12) --
   When asking the AI to implement business rules, validation chains,
   or transactional workflows, prepend "Think step by step" to get
   the model to reason through edge cases before writing code.
   ```
   Think step by step about the order cancellation flow:
   1. What states allow cancellation?
   2. What happens to inventory reservations?
   3. What events should be emitted?
   4. What should the rollback behavior be?
   Then implement OrderServiceImpl.cancel().
   ```

3. **Provide few-shot examples of your patterns** (Principle 7) --
   When generating a new endpoint, include an existing endpoint from
   your codebase as a reference example. The model will mirror the
   patterns (annotation ordering, DTO style, error handling) of the
   example rather than inventing its own.

4. **Use affirmative architecture rules** (Principle 4) -- In your
   `AGENTS.md` architecture section, phrase every rule as an
   affirmative directive. "Controllers return DTO records" is more
   reliably followed than "Controllers don't return entities."

## AI-Assisted Refactoring Patterns

These four refactoring patterns target areas where AI assistants excel
with Java 21. Each includes a before/after example and a ready-to-use
prompt.

### Pattern 1: Class → Record Conversion

Traditional POJOs with boilerplate can be collapsed into records while
preserving Jackson serialization and validation behavior.

**Before (pre-Java 16):**
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthDate;

    @Override
    public boolean equals(Object o) { /* ... */ }

    @Override
    public int hashCode() { /* ... */ }

    @Override
    public String toString() { /* ... */ }
}
```

**After (Java 21 record):**
```java
public record PersonDto(
    UUID id,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email String email,
    @Past LocalDate birthDate
) {
    public PersonDto {
        firstName = firstName.strip();
        lastName = lastName.strip();
    }
}
```

**Prompt:**
```
Refactor PersonDto from a class to a Java 21 record. Preserve Jackson
serialization compatibility. Add a compact canonical constructor that
strips whitespace from firstName and lastName. Move Bean Validation
annotations to record components. Show me any callers that need updating.
```

### Pattern 2: instanceof Chains → Pattern Matching

Replace fragile instanceof + cast chains with sealed interfaces and
exhaustive pattern matching switches.

**Before:**
```java
public BigDecimal calculateFee(Payment payment) {
    if (payment instanceof CreditCardPayment) {
        CreditCardPayment cc = (CreditCardPayment) payment;
        return cc.getAmount().multiply(CREDIT_CARD_FEE_RATE);
    } else if (payment instanceof BankTransferPayment) {
        BankTransferPayment bt = (BankTransferPayment) payment;
        return bt.getAmount().compareTo(BANK_TRANSFER_THRESHOLD) > 0
            ? FLAT_FEE : BigDecimal.ZERO;
    } else if (payment instanceof CryptoPayment) {
        CryptoPayment crypto = (CryptoPayment) payment;
        return crypto.getGasFee();
    } else {
        throw new IllegalArgumentException("Unknown payment type: " + payment);
    }
}
```

**After:**
```java
public sealed interface Payment permits CreditCardPayment, BankTransferPayment, CryptoPayment {
    BigDecimal amount();
}
public record CreditCardPayment(BigDecimal amount, String cardLast4) implements Payment {}
public record BankTransferPayment(BigDecimal amount, String iban) implements Payment {}
public record CryptoPayment(BigDecimal amount, BigDecimal gasFee) implements Payment {}

public BigDecimal calculateFee(Payment payment) {
    return switch (payment) {
        case CreditCardPayment cc   -> cc.amount().multiply(CREDIT_CARD_FEE_RATE);
        case BankTransferPayment bt -> bt.amount().compareTo(BANK_TRANSFER_THRESHOLD) > 0
            ? FLAT_FEE : BigDecimal.ZERO;
        case CryptoPayment crypto   -> crypto.gasFee();
    };
}
```

**Prompt:**
```
Refactor the PaymentProcessor.calculateFee() method to use a sealed
interface + pattern matching switch. The current code uses instanceof
checks for CreditCardPayment, BankTransferPayment, and CryptoPayment.
Convert each payment type to a record implementing the sealed interface.
The switch must be exhaustive (no default branch).
```

### Pattern 3: Manual Threading → Virtual Threads

Replace fixed thread pools with virtual threads and structured
concurrency for I/O-bound workloads.

> **Prerequisite on JDK 21–23** — see the version table at the top of the
> Virtual Threads section. Add `--enable-preview` to your Maven Compiler and
> Surefire plugins, or use `Executors.newVirtualThreadPerTaskExecutor()` with
> `CompletableFuture` for a flag-free Java 21 alternative.

**Before:**
```java
@Service
public class OrderServiceImpl implements OrderService {
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    @Override
    public List<OrderResult> processOrders(List<Order> orders) {
        List<Future<OrderResult>> futures = new ArrayList<>();
        for (Order order : orders) {
            futures.add(executor.submit(() -> processOne(order)));
        }
        List<OrderResult> results = new ArrayList<>();
        for (Future<OrderResult> f : futures) {
            try {
                results.add(f.get());
            } catch (Exception e) {
                results.add(OrderResult.failed(e));
            }
        }
        return results;
    }
}
```

**After:**
```java
@Service
public class OrderServiceImpl implements OrderService {

    @Override
    public List<OrderResult> processOrders(List<Order> orders)
            throws InterruptedException {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<StructuredTaskScope.Subtask<OrderResult>> subtasks = orders.stream()
                .map(order -> scope.fork(() -> processOne(order)))
                .toList();

            scope.join().throwIfFailed();

            return subtasks.stream()
                .map(StructuredTaskScope.Subtask::get)
                .toList();
        }
    }
}
```

**Prompt:**
```
Refactor OrderService.processOrders() to use virtual threads with
StructuredTaskScope. The current implementation uses a fixed thread
pool of 10 platform threads. Requirements:
1. Use StructuredTaskScope.ShutdownOnFailure for error handling.
2. Remove the fixed thread pool -- virtual threads are created per task.
3. Ensure proper resource cleanup with try-with-resources.
4. Keep the same method signature and return type.
```

### Pattern 4: Imperative → Stream Pipeline

Replace mutation-heavy for-loops with declarative stream pipelines
that are easier to parallelize and test.

**Before:**
```java
public MonthlyReport generateMonthlyReport(List<Order> orders, YearMonth month) {
    BigDecimal totalRevenue = BigDecimal.ZERO;
    int completedCount = 0;
    int cancelledCount = 0;
    Map<String, BigDecimal> revenueByCategory = new HashMap<>();

    for (Order order : orders) {
        if (!order.getCreatedAt().toLocalDate().getMonth().equals(month.getMonth())) {
            continue;
        }
        if (order.getStatus() == COMPLETED) {
            completedCount++;
            totalRevenue = totalRevenue.add(order.getTotal());
            String cat = order.getCategory();
            revenueByCategory.merge(cat, order.getTotal(), BigDecimal::add);
        } else if (order.getStatus() == CANCELLED) {
            cancelledCount++;
        }
    }
    return new MonthlyReport(totalRevenue, completedCount, cancelledCount, revenueByCategory);
}
```

**After:**
```java
public MonthlyReport generateMonthlyReport(List<Order> orders, YearMonth month) {
    var monthlyOrders = orders.stream()
        .filter(o -> YearMonth.from(o.getCreatedAt()).equals(month))
        .toList();

    var totalRevenue = monthlyOrders.stream()
        .filter(o -> o.getStatus() == COMPLETED)
        .map(Order::getTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    var completedCount = monthlyOrders.stream()
        .filter(o -> o.getStatus() == COMPLETED)
        .count();

    var cancelledCount = monthlyOrders.stream()
        .filter(o -> o.getStatus() == CANCELLED)
        .count();

    var revenueByCategory = monthlyOrders.stream()
        .filter(o -> o.getStatus() == COMPLETED)
        .collect(Collectors.groupingBy(
            Order::getCategory,
            Collectors.reducing(BigDecimal.ZERO, Order::getTotal, BigDecimal::add)
        ));

    return new MonthlyReport(totalRevenue, (int) completedCount, (int) cancelledCount, revenueByCategory);
}
```

**Prompt:**
```
Refactor the report generation in ReportService.generateMonthlyReport()
from imperative loops to a Stream pipeline. The current code uses for-loops
with mutable accumulators. Requirements:
1. Filter by month using YearMonth.from().
2. Use Collectors.groupingBy for revenue-by-category.
3. Keep the same MonthlyReport return type.
4. Do not introduce parallel streams unless the dataset exceeds 10K items.
```

## Testing Conventions for AI-Generated Code

When asking the AI to generate tests, include these conventions in
your prompt or `AGENTS.md` to avoid rework:

| Convention           | Rule                                                      |
|----------------------|-----------------------------------------------------------|
| Test class naming    | `{ClassName}Test` (e.g., `PersonServiceTest`)             |
| Test method naming   | `methodName_stateUnderTest_expectedBehavior`              |
| Structure            | Given/When/Then with `// given`, `// when`, `// then`     |
| Assertions           | AssertJ (`assertThat(...)`) over JUnit assertions         |
| Mocking              | Mockito with `@ExtendWith(MockitoExtension.class)`        |
| Integration tests    | `@SpringBootTest` with `@Testcontainers`                  |
| Controller tests     | `@WebMvcTest({ControllerClass}.class)` slice              |
| Repository tests     | `@DataJdbcTest` with Testcontainers PostgreSQL            |
| Test data            | ObjectMother pattern -- never hardcode UUIDs/timestamps   |
| Coverage target      | 80% line overall, 100% on service business logic          |

**Example prompt incorporating these conventions:**
```
Generate tests for PersonServiceImpl. Follow these rules:
- Test class: PersonServiceImplTest
- Naming: methodName_stateUnderTest_expectedBehavior
- Structure: given/when/then with section comments
- Use @ExtendWith(MockitoExtension.class), @Mock, @InjectMocks
- AssertJ assertions only (no assertEquals)
- Cover: happy path, null input, empty result, duplicate detection
- Use ObjectMother.personEntity() for test data
```

## Quick-Reference Cheat Sheet: Java 21

`java.util.SequencedCollection` is a new interface (JEP 431) that all
ordered collections (`List`, `LinkedHashSet`, `Deque`, etc.) implement.
It adds six methods so you never have to reach for index arithmetic or
`iterator().next()` to access the first or last element:

```java
List<String> names = List.of("Alice", "Bob", "Carol");

// Before Java 21 (verbose, error-prone)
String first = names.get(0);
String last  = names.get(names.size() - 1);

// Java 21
String first = names.getFirst();   // throws NoSuchElementException on empty
String last  = names.getLast();

// Reversed view (no copying)
SequencedCollection<String> reversed = names.reversed();
```

Prompt your AI with: `"Use SequencedCollection methods (getFirst/getLast/reversed) instead of index-based access in {method}"`.

### Feature-to-Prompt Table

| Java 21 Feature          | AI Prompt Template                                                                                   | When to Use                              |
|--------------------------|------------------------------------------------------------------------------------------------------|------------------------------------------|
| Records                  | "Refactor {Class} to a Java record. Preserve Jackson compatibility. Show callers that need updating" | Any DTO, value object, or event payload  |
| Sealed classes           | "Extract sealed interface from {Class} hierarchy. Make the switch exhaustive (no default)"           | Domain type hierarchies, event types     |
| Pattern matching switch  | "Replace instanceof chain in {method} with pattern matching switch using sealed types"               | Polymorphic dispatch, type-based routing |
| Virtual threads          | "Refactor {method} to use virtual threads with StructuredTaskScope. Remove the fixed thread pool"    | I/O-bound parallel work, HTTP clients    |
| Text blocks              | "Convert multi-line string concatenation in {method} to a text block"                                | SQL queries, JSON templates, HTML        |
| Stream + Collectors      | "Refactor for-loop in {method} to a Stream pipeline with Collectors.groupingBy"                      | Aggregation, filtering, transformation   |
| `Optional` improvements  | "Replace null checks in {method} with Optional.map/flatMap chain"                                    | Nullable return values, optional params  |
| `SequencedCollection`    | "Use SequencedCollection methods (getFirst/getLast/reversed) instead of index-based access in {method}" | First/last element access in lists   |


---

## Database — PostgreSQL, Oracle & Liquibase

### Why Database Safety Matters

Database changes are the only changes in a software project that can
**lose production data permanently**. A bad deployment rolls back in
90 seconds; a bad migration that drops a column or runs without a
transaction cannot be undone by a redeploy.

AI assistants generate plausible-looking SQL and Liquibase changesets
quickly, but "plausible" is not "safe". Without explicit guidance they
will:

- Use `VARCHAR` where your schema convention uses `TEXT`, causing silent
  truncation at the ORM layer.
- Omit `runInTransaction="false"` on `CREATE INDEX CONCURRENTLY`,
  making the changeset fail at startup.
- Generate `HikariCP` pool sizes using rules of thumb that do not fit
  your connection limits.
- Write JDBC-style queries when your team has standardised on Spring
  Data JDBC repositories.
- Suggest `@Query` with JPQL when your project uses native SQL exclusively.

This section exists because **one bad AI-generated migration reaching
production costs more than everything you saved in prompt round-trips
that sprint**. The configuration patterns here are a safety net, not a
convenience.

## Teaching the AI Your Database Patterns

### AGENTS.md Database Section

```markdown
## Database

### Technology
- Primary: PostgreSQL 15+
- Legacy: Oracle 19c (some services)
- Migrations: Liquibase (XML format)
- ORM: Spring Data JDBC (not JPA/Hibernate)
- Connection pool: HikariCP (Spring Boot default)

### Schema Conventions
- Table names: snake_case, plural (persons, orders, order_items)
- Column names: snake_case (first_name, created_at)
- Primary key: `id` (UUID type, generated by application)
- Foreign keys: {referenced_table_singular}_id (person_id)
- Timestamps: created_at, updated_at (TIMESTAMPTZ in PostgreSQL)
- Soft delete: deleted_at (nullable TIMESTAMPTZ)
- Indexes: idx_{table}_{column(s)} (idx_persons_email)
- Unique constraints: uq_{table}_{column(s)}
- Check constraints: chk_{table}_{description}

### Data Types (PostgreSQL)
| Concept      | PostgreSQL Type    | Java Type         |
|--------------|--------------------|-------------------|
| ID           | UUID               | java.util.UUID    |
| String       | VARCHAR(n)         | String            |
| Long text    | TEXT               | String            |
| Integer      | INTEGER            | int / Integer     |
| Money        | NUMERIC(19,4)      | BigDecimal        |
| Boolean      | BOOLEAN            | boolean           |
| Timestamp    | TIMESTAMPTZ        | Instant           |
| Date         | DATE               | LocalDate         |
| Enum         | VARCHAR(50)        | enum (Java)       |
| JSON         | JSONB              | String / JsonNode |

### Data Types (Oracle Equivalents)
| PostgreSQL    | Oracle Equivalent  |
|---------------|--------------------|
| UUID          | RAW(16)            |
| VARCHAR(n)    | VARCHAR2(n)        |
| TEXT          | CLOB               |
| BOOLEAN       | NUMBER(1)          |
| TIMESTAMPTZ   | TIMESTAMP WITH TIME ZONE |
| JSONB         | CLOB + IS JSON     |
| SERIAL        | GENERATED ALWAYS AS IDENTITY |

### Liquibase Conventions
- Format: XML
- Changelog master: db/changelog/db.changelog-master.xml
- Changelog files: db/changelog/changes/YYYY-MM-DD-NNN-description.xml
- Author: developer username
- Always include rollback
- One logical change per changeset
- Use preconditions for safety
- `CREATE INDEX CONCURRENTLY` requires `runInTransaction="false"` on the changeSet — PostgreSQL forbids CONCURRENTLY inside a transaction; omitting this attribute crashes Liquibase startup
```

## Liquibase Migration Patterns

### Standard Migration Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="2026-04-04-001-add-email-to-persons"
               author="developer">
        <preConditions onFail="MARK_RAN">
            <not>
                <columnExists tableName="persons" columnName="email"/>
            </not>
        </preConditions>

        <addColumn tableName="persons">
            <column name="email" type="VARCHAR(255)">
                <constraints nullable="true"/>
            </column>
        </addColumn>

        <createIndex indexName="idx_persons_email"
                     tableName="persons">
            <column name="email"/>
        </createIndex>

        <rollback>
            <dropIndex indexName="idx_persons_email"
                       tableName="persons"/>
            <dropColumn tableName="persons" columnName="email"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

### Common Migration Operations

| Operation          | Liquibase Tag         | Notes                      |
|--------------------|-----------------------|----------------------------|
| Create table       | `<createTable>`       | Always include PK and timestamps |
| Add column         | `<addColumn>`         | Include precondition       |
| Drop column        | `<dropColumn>`        | Dangerous -- precondition required |
| Add index          | `<createIndex>`       | Name: idx_{table}_{cols}   |
| Add foreign key    | `<addForeignKeyConstraint>` | Name: fk_{src}_{ref} |
| Add unique         | `<addUniqueConstraint>` | Name: uq_{table}_{cols} |
| Data migration     | `<sql>`               | Use CDATA, include rollback |
| Rename column      | `<renameColumn>`      | Check all queries first    |

### Safety Rules for Migrations

```markdown
## Migration Safety Rules

### ALWAYS
- Include rollback statements
- Use preconditions to prevent double-execution
- Test migrations against a fresh database AND existing data
- Run ./mvnw liquibase:validate before committing

### NEVER
- Drop a column without a precondition check
- Rename a table in production without a multi-step migration
- Add NOT NULL without a default value (breaks existing rows)
- Modify existing changeset IDs (Liquibase checksums will fail)
- Use auto-generated IDs (use descriptive IDs like dates)
```

## Query Optimization

### Teaching the AI to Write Efficient SQL

```markdown
## SQL Performance Rules

### Indexing
- Every foreign key column must be indexed
- Columns used in WHERE clauses frequently should be indexed
- Composite indexes: most selective column first
- Don't over-index: each index slows writes

### Query Patterns
- Use EXPLAIN ANALYZE to verify query plans
- Prefer JOIN over subquery for correlated queries
- Use EXISTS instead of IN for large subsets
- Avoid SELECT * -- list required columns explicitly
- Use LIMIT/OFFSET or keyset pagination (not OFFSET for large tables)

### Spring Data JDBC Queries
- Use method naming for simple queries: findByEmail(String email)
- Use @Query for complex queries with named parameters
- Never concatenate SQL strings
- Use RowMapper for custom result mapping
```

### PostgreSQL-Specific Optimization

```markdown
### PostgreSQL Tips
- Use JSONB for flexible schema fields (not JSON)
- Use partial indexes for filtered queries:
  CREATE INDEX idx_active_persons ON persons(email) WHERE deleted_at IS NULL;
- Use CONCURRENTLY for index creation in production
- Use pg_stat_user_tables to find missing indexes
- Use VACUUM ANALYZE after large data changes
- Connection pool: HikariCP with maximumPoolSize = (CPU cores * 2) + disk spindles
```

### Oracle-Specific Considerations

```markdown
### Oracle Tips
- Use Oracle hints sparingly and document when used
- Prefer FETCH FIRST N ROWS instead of ROWNUM for pagination
- Use bind variables (Spring parameterized queries handle this)
- Check execution plans with EXPLAIN PLAN FOR
- Use AWR reports for production query analysis
- RAW(16) for UUID storage (convert in application layer)
```

## Database Skill

```markdown
---
name: db-migration
description: >
  Generate Liquibase database migration changelogs. Use when asked to
  create migrations, add columns, create tables, add indexes, or modify
  database schema. Handles PostgreSQL and Oracle syntax.
---

## Instructions

When generating Liquibase changelogs:

1. Use XML format
2. Changeset ID: YYYY-MM-DD-NNN-description
3. Author: use "ai-assistant" (developer will update)
4. Always include preconditions
5. Always include rollback
6. Use project naming conventions for tables, columns, indexes
7. Place file in: src/main/resources/db/changelog/changes/
8. Add include to db.changelog-master.xml

## PostgreSQL Types
Use PostgreSQL types by default. Add Oracle alternatives in comments.

## Safety Checks
Before generating a destructive migration (DROP, RENAME):
1. Warn the user about data loss risk
2. Suggest a safer multi-step approach
3. Include preconditions to prevent accidental execution
```

## AI-Assisted Query Optimization Workflow

A step-by-step workflow for optimizing SQL queries with AI assistance:

1. **Capture the slow query**: Get the actual SQL from application logs
   or `pg_stat_statements`:
   ```sql
   SELECT query, mean_exec_time, calls
   FROM pg_stat_statements
   ORDER BY mean_exec_time DESC
   LIMIT 10;
   ```

2. **Get the execution plan**: Run `EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)`
   and copy the full output:
   ```sql
   EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
   SELECT p.id, p.first_name, p.last_name, p.email
   FROM persons p
   WHERE p.status = 'ACTIVE'
     AND p.created_at > '2025-01-01'
   ORDER BY p.last_name
   LIMIT 20;
   ```

3. **Ask AI to analyze**: Paste the plan and add context:
   > "Analyze this PostgreSQL execution plan. Identify the bottleneck
   > and suggest index changes. The `persons` table has 2M rows,
   > roughly 60% are ACTIVE. We use Spring Data JDBC."

4. **Review suggested indexes**: Before applying, verify against existing
   indexes:
   ```sql
   SELECT indexname, indexdef
   FROM pg_indexes
   WHERE tablename = 'persons';
   ```

5. **Generate migration**: Use the `/migration` command:
   > "/migration — add a composite index on persons(status, created_at, last_name)
   > for the active-persons query optimization"

6. **Benchmark**: Re-run the query and compare execution plans.
   Confirm the planner uses the new index.

### Concrete Example

**Slow query** — fetching recently created active persons:

```sql
-- Before: Seq Scan on persons (cost=0.00..98754.00 rows=612000 width=128)
--         Planning Time: 0.2ms  Execution Time: 1340ms
SELECT id, first_name, last_name, email
FROM persons
WHERE status = 'ACTIVE'
  AND created_at > '2025-01-01'
ORDER BY last_name
LIMIT 20;
```

**AI-suggested optimization**:

```sql
-- Composite index covering the WHERE and ORDER BY
CREATE INDEX CONCURRENTLY idx_persons_status_created_last
ON persons (status, created_at, last_name);

-- After: Index Scan using idx_persons_status_created_last
--        Planning Time: 0.3ms  Execution Time: 2.1ms
```

**Generated Liquibase changeset**:

```xml
<!--
  IMPORTANT: CREATE INDEX CONCURRENTLY cannot run inside a transaction.
  runInTransaction="false" is mandatory — Liquibase will fail at startup
  if you omit it and PostgreSQL rejects the statement.
-->
<changeSet id="2026-04-10-001-idx-persons-active-query"
           author="ai-assistant"
           runInTransaction="false">
    <preConditions onFail="MARK_RAN">
        <not>
            <indexExists indexName="idx_persons_status_created_last"/>
        </not>
    </preConditions>

    <sql>
        CREATE INDEX CONCURRENTLY idx_persons_status_created_last
        ON persons (status, created_at, last_name);
    </sql>

    <rollback>
        -- CONCURRENTLY cannot run in a transaction either, so we use the
        -- blocking form for rollback (acceptable: rollbacks run in maintenance windows).
        DROP INDEX IF EXISTS idx_persons_status_created_last;
    </rollback>
</changeSet>
```

## Database Anti-Patterns AI Can Catch

Six common anti-patterns and the prompts to detect them:

### 1. N+1 Query Patterns in Spring Data JDBC

Occurs when a parent query triggers individual queries for each child.

> **Prompt**: "Review this Spring Data JDBC repository and service layer.
> Check for N+1 query patterns — places where we load a list of entities
> and then query related data in a loop. Suggest `@Query` with JOINs or
> batch loading instead."

### 2. Missing Indexes on Foreign Keys

Every foreign key column should be indexed for JOIN performance.

> **Prompt**: "Compare my schema against this rule: every column ending
> in `_id` (foreign key) must have an index. List columns that violate
> this rule and generate Liquibase changesets to add the missing indexes."

### 3. Using `SELECT *` Instead of Projections

Fetches unnecessary columns, wastes network and memory.

> **Prompt**: "Scan all `@Query` annotations and SQL statements in the
> repository layer. Flag any that use `SELECT *` and rewrite them to
> select only the columns needed by the calling service method."

### 4. Missing `NOT NULL` Constraints

Columns that should never be null lack database-level enforcement.

> **Prompt**: "Review the persons table schema. Identify columns that
> logically should never be null (e.g., first_name, last_name, status,
> created_at) but are currently nullable. Generate a Liquibase migration
> to add NOT NULL constraints with appropriate default values for
> existing rows."

### 5. Incorrect Data Types

Using wrong types leads to implicit casts and index bypasses.

> **Prompt**: "Check the schema for data type mismatches: VARCHAR used
> for dates or timestamps, INTEGER used for monetary values, TEXT used
> where VARCHAR with a length constraint would be safer. Suggest
> corrections with migration changesets."

### 6. Missing Rollback Blocks in Liquibase Changesets

Changesets without rollback blocks make releases irreversible.

> **Prompt**: "Scan all Liquibase changelog files in
> `src/main/resources/db/changelog/changes/`. List any changesets that
> are missing `<rollback>` blocks and generate the missing rollback
> statements for each."

## Schema Review Command Example

The `/schema-review` command performs a comprehensive schema health check:

```markdown
/schema-review

Analyze the database schema defined in the Liquibase changelogs
under `src/main/resources/db/changelog/changes/` and the current
database DDL. Check for these issues:

### Naming Convention Violations
- Tables must be snake_case, plural (persons, order_items)
- Columns must be snake_case (first_name, created_at)
- Primary keys must be named `id`
- Foreign keys must follow {referenced_table_singular}_id pattern
- Indexes: idx_{table}_{columns}
- Unique constraints: uq_{table}_{columns}

### Missing Indexes
- Every foreign key column (_id suffix) must have an index
- Columns used in WHERE clauses of frequent queries should be indexed
- Composite indexes should have the most selective column first

### Nullable Columns That Should Be NOT NULL
- Primary keys (id)
- Timestamps (created_at, updated_at)
- Status/type columns (status, type)
- Core business fields (first_name, last_name for persons)

### Missing Foreign Key Constraints
- Columns ending in _id should have a corresponding foreign key constraint
- Verify ON DELETE behavior (CASCADE vs RESTRICT vs SET NULL)

### Table/Column Documentation Gaps
- Every table should have a COMMENT ON TABLE
- Non-obvious columns should have a COMMENT ON COLUMN
- Enum-like VARCHAR columns should document valid values

Output:
1. A table of all issues found (severity, table, column, issue, fix)
2. Liquibase changesets to fix each issue
3. Summary statistics (tables checked, issues by severity)
```

## Quick-Reference Cheat Sheet: Database
| Add column | "/migration — add {column} ({type}) to {table}" | AI may omit precondition or rollback |
| Add index | "Create an index on {table}({columns}) for optimizing {query description}" | AI may not use `CONCURRENTLY` for production indexes |
| Optimize query | "Analyze this EXPLAIN plan: {plan}. Table has {N} rows." | AI may suggest indexes without checking existing ones |
| Schema review | "/schema-review" | AI may miss cross-table relationship issues |
| Generate migration | "/migration — {description}" | AI may use wrong changelog ID format |
| N+1 check | "Check repository {name} for N+1 query patterns" | AI may miss indirect N+1 through service-layer loops |
| Oracle migration | "Generate this migration for Oracle 19c: {PG changeset}" | AI may forget RAW(16) for UUID or NUMBER(1) for BOOLEAN |


---

## DevOps — GitLab CI & Kubernetes

### Why DevOps Verification Matters

DevOps YAML is deceptively simple to write and expensive to debug.
A `gitlab-ci.yml` with a wrong stage name silently orphans a job.
A Kubernetes manifest with the wrong resource units deploys, runs
fine in staging, and OOMKills in production under real traffic.

These are not hypothetical risks — they are the most common class of
incident in teams that adopt AI-generated infrastructure without a
verification layer. The failure mode is insidious: the AI is
*approximately correct*, which means the error survives review.

There are two structural problems AI assistants have with DevOps work:

1. **Your environment is unique.** Every company has different shared CI
   template paths, different Helm chart conventions, different Kubernetes
   namespace and resource quota policies. Generic AI training data is
   useless for this. Until you teach the AI your specific environment,
   every suggestion needs full manual verification.

2. **The feedback loop is slow.** A wrong Java class fails in seconds.
   A wrong CI pipeline fails in 10–20 minutes, after it has already
   consumed a runner. A wrong Kubernetes manifest may not fail at all
   until load hits it. AI is most useful here when it generates
   *debuggable artefacts*, not when it produces *opaque YAML*.

This section shows you how to close both gaps: give the AI enough
context about your infrastructure that its suggestions are accurate,
and use it as a debugging partner that explains its reasoning step by step.

## GitLab CI with AI Assistants

### Teaching the AI Your CI Structure

Add a GitLab CI section to `AGENTS.md`:

```markdown
## GitLab CI

### Pipeline Structure
- Configuration: .gitlab-ci.yml in project root
- Uses shared templates via `include:`
- Project-specific variables defined under `variables:`

### Template Pattern
\```yaml
include:
  - project: 'platform/ci-templates'
    ref: main
    file:
      - '/templates/java-maven.yml'
      - '/templates/docker-build.yml'
      - '/templates/helm-deploy.yml'

variables:
  JAVA_VERSION: "21"
  CHART_NAME: "my-service"
  MAVEN_OPTS: "-Xmx1024m"
\```

### Stages
1. build: Compile and package (./mvnw clean package)
2. test: Run tests with coverage (./mvnw test jacoco:report)
3. docker: Build and push container image
4. deploy-dev: Deploy to dev via Helm
5. deploy-staging: Deploy to staging (manual trigger)
6. deploy-prod: Deploy to prod (manual trigger + approval)

### Rules
- Never hardcode secrets in .gitlab-ci.yml
- Use CI/CD variables for environment-specific values
- Always include the shared templates; do not duplicate their logic
- Use `rules:` (not `only:`/`except:`) for job conditions
```

### GitLab CI Skill

Create a skill for generating CI pipeline configurations:

```markdown
---
name: gitlab-ci
description: >
  Generate or modify GitLab CI pipeline configurations. Use when asked
  to create pipelines, add CI stages, fix CI failures, or configure
  deployment jobs. Covers .gitlab-ci.yml syntax and best practices.
---

## Instructions

When generating GitLab CI configurations:

1. Always use `include:` for shared templates
2. Use `rules:` instead of `only:`/`except:`
3. Define variables at the top level
4. Use anchors (&, *) for repeated configuration
5. Never embed secrets -- use $CI_VARIABLE_NAME
6. Add `needs:` for parallel job execution
7. Use `artifacts:` to pass data between stages
8. Include `cache:` for Maven dependencies

## Template

\```yaml
include:
  - project: 'platform/ci-templates'
    ref: main
    file: '/templates/java-maven.yml'

variables:
  CHART_NAME: "${SERVICE_NAME}"

stages:
  - build
  - test
  - deploy
\```
```

### GitLab CI Command

Create `/ci-fix` to help debug pipeline failures:

```markdown
Analyze the GitLab CI pipeline failure I'm describing.

1. Identify the failing stage and job
2. Determine root cause (compilation, test, Docker, deployment)
3. Suggest a fix with the exact YAML change needed
4. If the fix requires a new CI variable, document it

Consider common issues:
- Maven dependency resolution failures (check mirrors/proxies)
- Docker build context issues (check .dockerignore)
- Helm deployment failures (check values files and secrets)
- Network issues (check runner tags and network policies)
```

## Kubernetes & Helm with AI Assistants

### Teaching the AI Your K8s Patterns

Add Kubernetes patterns to `AGENTS.md`:

```markdown
## Kubernetes / Helm

### Helm Chart Structure
\```
.helm/
├── Chart.yaml
├── values.yaml          # Default values
├── values-dev.yaml      # Dev overrides
├── values-staging.yaml  # Staging overrides
├── values-prod.yaml     # Production overrides
└── templates/
    ├── deployment.yaml
    ├── service.yaml
    ├── ingress.yaml
    ├── configmap.yaml
    ├── secret.yaml
    └── hpa.yaml
\```

### Deployment Requirements
- Always include liveness and readiness probes
- Set resource requests AND limits
- Use securityContext (non-root, readOnlyRootFilesystem)
- Environment variables from ConfigMap/Secret references
- Pod disruption budget for production
- Horizontal Pod Autoscaler for production

### Probe Configuration
\```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: http
  initialDelaySeconds: 30
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: http
  initialDelaySeconds: 10
  periodSeconds: 5
\```

### Resource Defaults
\```yaml
resources:
  requests:
    cpu: 200m
    memory: 512Mi
  limits:
    cpu: "1"
    memory: 1Gi
\```

### Naming Convention
- Release name: {service-name}-{environment}
- Labels: app.kubernetes.io/name, app.kubernetes.io/instance
- Annotations: team, contact, cost-center
```

### Helm Skill

```markdown
---
name: helm-template
description: >
  Generate or modify Helm chart templates for Kubernetes deployments.
  Use when asked to create K8s manifests, add probes, configure
  resources, or set up ingress. Follows project Helm conventions.
---

## Instructions

When generating Helm templates:

1. Use .helm/ directory structure (not charts/)
2. Always include liveness and readiness probes using Spring Actuator
3. Set resource requests and limits
4. Use securityContext with non-root user
5. Environment variables via configMapRef and secretRef
6. Use {{ .Values.xxx }} for all configurable values
7. Include proper labels (app.kubernetes.io/*)
8. Add comments for non-obvious configurations
```

### K8s Debugging Command

Create `/k8s-debug` for troubleshooting:

```markdown
Help me debug a Kubernetes deployment issue.

Analyze the symptoms I describe and suggest:
1. Diagnostic kubectl commands to run
2. Common root causes for the symptom
3. Fixes ranked by likelihood

Common patterns:
- CrashLoopBackOff: Check logs, probe config, resource limits, JVM heap
- OOMKilled: Increase memory limit, check JVM -Xmx setting
- ImagePullBackOff: Check image name, registry credentials, tag
- Pending pod: Check node resources, node selector, tolerations
- Readiness probe failure: Check actuator endpoint, startup time
```

## Environment-Specific Values

Teach the AI how your environments differ:

```markdown
## Environment Differences

| Setting         | Dev            | Staging          | Production       |
|-----------------|----------------|------------------|------------------|
| Replicas        | 1              | 2                | 3+               |
| Resources (CPU) | 100m / 500m    | 200m / 1         | 500m / 2         |
| Resources (Mem) | 256Mi / 512Mi  | 512Mi / 1Gi      | 1Gi / 2Gi        |
| HPA             | disabled       | min:2, max:4     | min:3, max:10    |
| Ingress TLS     | self-signed    | Let's Encrypt    | corporate CA     |
| Log level       | DEBUG          | INFO             | WARN             |
| DB pool size    | 5              | 10               | 20               |
```

## AI-Assisted Pipeline Debugging

When a CI pipeline fails, follow this structured workflow to get the
most accurate help from your AI assistant:

### Debugging Workflow

1. **Copy the error output** from GitLab CI — include the full job log,
   not just the last line. Relevant context often appears earlier.

2. **Include the relevant `.gitlab-ci.yml` section** — the AI needs to
   see the job definition, variables, and any `include:` references.

3. **Ask with context**:
   > "This GitLab CI job failed in the 'test' stage. Here's the error
   > output and the job definition. Diagnose the root cause and suggest
   > a fix."
   >
   > ```
   > [ERROR] Failed to execute goal org.apache.maven.plugins:
   > maven-surefire-plugin:3.2.5:test ... Timeout
   > ```
   >
   > ```yaml
   > test:
   >   stage: test
   >   script:
   >     - ./mvnw test jacoco:report
   >   artifacts:
   >     reports:
   >       junit: target/surefire-reports/*.xml
   > ```

### Common CI Failures AI Can Help Debug

| Failure Type | Symptoms | Key Context to Include |
|-------------|----------|----------------------|
| Maven dependency resolution | `Could not resolve dependencies`, `Non-resolvable parent POM` | `pom.xml` dependencies section, `settings.xml` mirror config |
| Docker build | `COPY failed`, `RUN apt-get ... E: Unable to locate package` | `Dockerfile`, `.dockerignore`, base image version |
| Kubernetes deployment | `ImagePullBackOff`, `ErrImagePull` | Image name/tag, registry URL, deployment YAML |
| Helm template rendering | `Error: template ... not defined`, `cannot load values` | `values.yaml`, relevant template file, `Chart.yaml` |
| Test failures in CI only | Tests pass locally but fail in CI | CI runner environment, JVM version, `MAVEN_OPTS`, timezone settings |

### Prompt Template for CI Debugging

```markdown
My GitLab CI pipeline failed. Here's the context:

**Stage**: {stage name}
**Job**: {job name}
**Runner tags**: {runner tags if relevant}

**Error output**:
\```
{paste error log}
\```

**Job definition** (.gitlab-ci.yml):
\```yaml
{paste job YAML}
\```

**Relevant files** (if applicable):
- Dockerfile / pom.xml / Helm values

Diagnose the root cause. Suggest a fix with the exact file changes needed.
If the fix requires a new CI variable, specify the variable name and
expected value.
```

## Deployment Pipeline Generation

How to generate a complete deployment pipeline with AI assistance:

### Step 1: Describe Your Requirements

> "I need a GitLab CI pipeline for a Spring Boot 3.5 app that:
> - Builds with Maven 3.9+ and Java 21
> - Runs unit tests with JaCoCo coverage (minimum 80%)
> - Builds a Docker image and pushes to our GitLab Container Registry
> - Deploys to K8s via Helm with environment-specific values
> - Has manual approval gates for staging and production
> - Caches Maven dependencies between runs
> - Uses our shared templates from platform/ci-templates"

### Step 2: Review the Generated Pipeline

The AI should produce a `.gitlab-ci.yml` similar to:

```yaml
include:
  - project: 'platform/ci-templates'
    ref: main
    file:
      - '/templates/java-maven.yml'
      - '/templates/docker-build.yml'
      - '/templates/helm-deploy.yml'

variables:
  JAVA_VERSION: "21"
  CHART_NAME: "my-service"
  MAVEN_OPTS: "-Xmx1024m -Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

stages:
  - build
  - test
  - docker
  - deploy-staging
  - deploy-production

cache:
  key: "${CI_COMMIT_REF_SLUG}"
  paths:
    - .m2/repository/

build:
  stage: build
  script:
    - ./mvnw clean package -DskipTests
  artifacts:
    paths:
      - target/*.jar
    expire_in: 1 hour

test:
  stage: test
  script:
    - ./mvnw test jacoco:report
    - awk -F ',' '{ missed += $4; covered += $5 }
      END { pct = 100 * covered / (missed + covered);
      printf "Coverage: %.1f%%\n", pct;
      if (pct < 80) exit 1 }' target/site/jacoco/jacoco.csv
  artifacts:
    reports:
      junit: target/surefire-reports/*.xml
    paths:
      - target/site/jacoco/

docker:
  stage: docker
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
  rules:
    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH

deploy-staging:
  stage: deploy-staging
  script:
    - helm upgrade --install $CHART_NAME-staging .helm/
        --values .helm/values-staging.yaml
        --set image.tag=$CI_COMMIT_SHA
        --namespace staging
  environment:
    name: staging
  rules:
    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH
      when: manual

deploy-production:
  stage: deploy-production
  script:
    - helm upgrade --install $CHART_NAME-prod .helm/
        --values .helm/values-prod.yaml
        --set image.tag=$CI_COMMIT_SHA
        --namespace production
  environment:
    name: production
  rules:
    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH
      when: manual
      allow_failure: false
```

### Step 3: Generate Environment-Specific Helm Values

Ask the AI to generate values files for each environment:

> "Generate Helm values files for staging and production using our
> environment differences table (replicas, resources, HPA, log level,
> DB pool size). Use the project's values structure."

### Step 4: Add Manual Approval Gates

For production deployments, ensure the pipeline includes:
- `when: manual` on the production deploy job
- `allow_failure: false` to block the pipeline until approved
- Consider adding a `needs:` dependency on staging deployment
- Optionally configure GitLab protected environments for additional control

## Common AI Mistakes in CI/CD and How to Catch Them

Understanding the failure modes lets you prompt defensively and review
AI output faster.

| Mistake | Why It Happens | How to Catch It |
|---------|---------------|-----------------|
| Job references a stage not in `stages:` list | AI forgets to sync the stage list with new jobs | `gitlab-ci-lint` or `yq '.stages' .gitlab-ci.yml` |
| Cache key too broad | AI uses `$CI_COMMIT_REF_SLUG` — different branches never share cache | Check `key:` is `$CI_COMMIT_REF_SLUG-$CI_JOB_NAME` |
| `needs:` creates a cycle | AI adds `needs:` dependencies without checking direction | Run `gitlab-ci-lint` — it catches DAG cycles |
| `artifacts: expire_in` omitted | Artifacts accumulate indefinitely, filling runner storage | Always specify `expire_in: 1 hour` for build artefacts |
| Docker build uses `latest` tag | AI defaults to `:latest` — no traceability, breaks rollback | Always use `$CI_COMMIT_SHA` as the image tag |
| Production deploy is not `when: manual` | AI generates `when: on_success` by default | Verify every production job has `when: manual` |
| Resource requests absent from Helm templates | AI omits `resources:` — pod gets `BestEffort` QoS class | Always verify `requests` and `limits` are both set |
| `initialDelaySeconds` too short | AI guesses 30 s; JVM warm-up on cold nodes often takes 60–90 s | Test actual startup time with `kubectl logs -f` |

**Prompt to validate generated pipelines:**
```
Review this .gitlab-ci.yml for common mistakes:
1. Are all job stage names present in the top-level stages list?
2. Does every job that builds an artifact set expire_in?
3. Are production deploy jobs gated with when: manual?
4. Are there any needs: dependencies that form a cycle?
5. Is the Docker image tagged with the commit SHA, not latest?

{paste .gitlab-ci.yml content}
```

## Incident Debugging with AI

How to use AI during a production incident to accelerate diagnosis:

### Step 1: Gather Evidence

Collect these outputs before asking the AI:

```bash
# Pod logs (last 100 lines)
kubectl logs -n prod deployment/my-service --tail=100

# Pod description (events, resource usage, restart count)
kubectl describe pod -n prod <pod-name>

# Resource usage
kubectl top pod -n prod -l app=my-service

# Recent events in the namespace
kubectl get events -n prod --sort-by='.lastTimestamp' | tail -20
```

### Step 2: Ask with Full Context

> "This Spring Boot service in Kubernetes is crashing with OOMKilled.
> Here are the logs, pod description, and resource metrics.
>
> **Pod logs**:
> ```
> {paste logs}
> ```
>
> **Pod describe** (relevant section):
> ```
> {paste describe output — especially Last State, Restart Count, Limits}
> ```
>
> **Resource usage**:
> ```
> {paste kubectl top output}
> ```
>
> What's the likely cause? How should I adjust the JVM and container
> resources? Current settings: container limit 1Gi, JVM -Xmx not set."

### Step 3: Common Incident Patterns

| Symptom | Likely Cause | AI Can Help With |
|---------|-------------|-----------------|
| OOMKilled | JVM heap exceeds container memory limit | Calculating correct `-Xmx` relative to container limit (75% rule) |
| CrashLoopBackOff | Application fails to start, probe timeout | Analyzing startup logs, adjusting `initialDelaySeconds` |
| High latency | Connection pool exhaustion, slow queries | Analyzing thread dumps, HikariCP metrics |
| 5xx errors after deploy | Incompatible schema migration, config error | Comparing new vs old config, checking migration status |
| Pod stuck Pending | Insufficient cluster resources, node affinity | Checking node capacity, suggesting resource adjustments |

### Step 4: Generate the Fix

Once root cause is identified, ask the AI to generate the fix:

> "The root cause is that JVM heap is not bounded and grows beyond the
> 1Gi container limit. Generate:
> 1. Updated Helm values with correct resource limits and JVM flags
> 2. A ConfigMap entry for JAVA_TOOL_OPTIONS
> 3. The kubectl commands to apply the hotfix now"

## Quick-Reference Cheat Sheet: DevOps
| Debug CI failure | "This CI job failed in '{stage}'. Error: {error}. Job YAML: {yaml}" | Full error log, job definition, referenced template names |
| Generate Helm chart | "Generate a Helm chart for {service} following our conventions" | `.helm/` structure, probe paths, resource defaults, environment table |
| Debug K8s issue | "/k8s-debug — {symptom}. Logs: {logs}. Describe: {describe}" | Pod logs, describe output, recent events, resource metrics |
| Add CI stage | "Add a {stage-name} stage to this pipeline: {existing yaml}" | Where it fits in stage order, dependencies via `needs:` |
| Hotfix deploy | "Generate kubectl commands to patch deployment {name} in {namespace}" | Current image tag, new image tag, rollback strategy |
| Security scan | "Add dependency-check and SAST stages to this pipeline" | Approved scanner images, fail thresholds, report formats |
| Helm values | "/helm-values — generate {env} values for {service}" | Environment differences table, service-specific overrides |


---

## Next Section

Proceed to [Section 16: Team Collaboration](16-team-collaboration.md) to learn
how to share AI configurations and onboard developers as a team.

### Further Reading

- [Section 9: AGENTS.md](09-agents-md.md) — project context for all three parts
- [Section 10: Skills](10-skills.md) — `db-migration`, `ci-fix`, `k8s-debug` skills
- [Section 11: Custom Commands](11-custom-commands.md) — `/migration`, `/k8s-debug`
- [Section 6: Context](06-context.md) — token budget for large schema/CI prompts
- [templates/sample-project/](templates/sample-project/) — reference implementation
