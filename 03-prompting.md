# Section 3: Prompt Engineering — Fundamentals and Management

Prompt engineering is the discipline of structuring natural-language instructions
so that a large language model consistently produces the output you need.
A well-structured prompt can improve response quality by over 50% and
correctness by over 35% compared to a naive request (Bsharat et al., 2024).

This section teaches research-backed prompting techniques for AI-assisted
Java/Spring Boot development, then covers **multi-project prompt management** —
storage layers, sharing, versioning, and Linux tooling.

| Section | Topic |
|---------|-------|
| [Why Prompting Matters](#why-prompting-matters-for-developers) | Value, impact, the prompting gap |
| [Anatomy of a Prompt](#anatomy-of-an-effective-prompt) | Role, Task, Context, Format |
| [CO-STAR Framework](#the-co-star-framework) | Structured six-element prompt design |
| [26 Principles](#the-26-principles-of-effective-prompting) | Research-backed prompting rules |
| [Applying to Config Files](#applying-prompting-techniques-to-assistant-configuration) | AGENTS.md, SKILL.md, custom commands |
| [Practical Examples](#practical-examples-each-principle-applied) | 14 principles applied to Java |
| [Anti-Patterns](#prompting-anti-patterns-for-java-developers) | Common mistakes and fixes |
| [Multi-Project Prompt Management](#multi-project-prompt-management) | Storage layers, Git, Stow, chezmoi, versioning |

---

## Why Prompting Matters for Developers

Without prompting skills, you end up in a frustrating cycle:
1. Ask the AI to "write a REST endpoint"
2. Get Java 8-style code with public fields and no validation
3. Spend 20 minutes fixing what it produced
4. Wonder if the AI is worth using at all

With good prompts, the same assistant generates idiomatic Java 21 code
that follows your project's conventions on the first try.

The difference is not the model -- it is the prompt.

---

## Anatomy of an Effective Prompt

Every prompt has four building blocks:

```text
┌─────────────────────────────────────────────────────────┐
│  ROLE       Who the model should act as                 │
│  TASK       What exactly you want done                  │
│  CONTEXT    Background, constraints, relevant data      │
│  FORMAT     The exact shape of the output               │
└─────────────────────────────────────────────────────────┘
```

- **Role** -- The expert persona the model should embody.
- **Task** -- The specific action: analyze, generate, refactor, review.
- **Context** -- Domain information, constraints, prior decisions.
- **Format** -- The structure and style of the expected output.

These map directly onto AGENTS.md sections (Section 8), SKILL.md bodies
(Section 9), and custom commands (Section 10). When you write a skill
instruction like "Review this code for SOLID violations and present
findings grouped by severity," you are applying this anatomy.

---

## The CO-STAR Framework

CO-STAR is a practical mnemonic that condenses prompting best practice
into six checkpoints. It won Singapore's GPT-4 Prompt Engineering
Competition (Sheila Teo, 2024) and provides a quick mental checklist
before sending any prompt.

| Letter | Element   | Question to Ask Yourself                           |
|--------|-----------|----------------------------------------------------|
| **C**  | Context   | What background does the model need?               |
| **O**  | Objective | What specific task should it perform?               |
| **S**  | Style     | What writing or coding style should it follow?      |
| **T**  | Tone      | What attitude should the response convey?           |
| **A**  | Audience  | Who will read or use this output?                   |
| **R**  | Response  | What format should the output take?                 |

### CO-STAR in Practice: Java Examples

**Bad prompt (no structure):**
```
Create a person endpoint.
```

**CO-STAR prompt:**
```
Context: Spring Boot 3.5 project using Java 21, Spring Data JDBC,
PostgreSQL. Package: com.example.myservice.

Objective: Create a REST endpoint for creating a new Person entity.

Style: Follow layered architecture -- Controller delegates to Service,
Service uses Repository. Use Java records for DTOs. Apply @Valid on
request bodies.

Tone: Production-quality code, no TODOs or placeholders.

Audience: Senior Java developers reviewing a merge request.

Response: Produce these files in order:
1. CreatePersonRequest.java (record with validation)
2. PersonResponse.java (record)
3. PersonService.java (interface)
4. PersonServiceImpl.java
5. PersonController.java (with OpenAPI annotations)
```

The second prompt is longer, but it produces correct output on the first
attempt. The extra 30 seconds of writing saves 20 minutes of fixing.

---

## The 26 Principles of Effective Prompting

Bsharat et al. (2024) identified 26 principles that measurably improve
LLM response quality. We group them into the same five categories used
in the original research, with coding-specific examples for each.

### Category 1: Prompt Structure and Clarity

These principles control how the model interprets the structure of
your request.

**Principle: Integrate the intended audience**

Tell the model who will read the output. This changes vocabulary,
detail level, and assumptions.

```
# Without audience
Explain what @Transactional does.

# With audience
Explain what @Transactional does. The audience is a mid-level
Java developer who has used JDBC but never Spring's transaction
management.
```

**Principle: Use affirmative directives**

State what the model should do, not what it should avoid. Negative
instructions ("don't use Lombok") are processed less reliably than
positive ones ("use manual constructors and getters").

```
# Negative (less reliable)
Don't use raw types. Don't put business logic in controllers.
Don't return entities from endpoints.

# Affirmative (more reliable)
Use parameterized types for all collections.
Place business logic in @Service classes only.
Return DTO records from all REST endpoints.
```

This is directly applicable to `AGENTS.md` -- write your architecture
rules as affirmative "do this" statements rather than "don't do that"
prohibitions (see Section 8).

**Principle: Use delimiters**

Separate sections of your prompt with visual markers so the model
knows where instructions end and data begins.

```
###Instruction###
Review the following Java class for SOLID violations.

###Code###
public class PersonServiceImpl implements PersonService {
    // ...
}

###Output Format###
List each violation with: principle violated, line number, suggested fix.
```

**Principle: Use leading words ("think step by step")**

Chain-of-thought prompting forces the model to reason before answering.
This is especially effective for complex logic, debugging, and
architectural decisions.

```
# Without CoT
What is wrong with this SQL query?

# With CoT
Analyze this SQL query step by step:
1. Check the JOIN conditions for correctness
2. Verify the WHERE clause logic
3. Look for N+1 or missing index patterns
4. Assess the SELECT for unnecessary columns
Then summarize the issues found.
```

**Principle: Use output primers**

End your prompt with the beginning of the expected output. This anchors
the model's response format.

```
Generate a Liquibase changelog for adding an email column to the
person table.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
```

The model will continue in the established format rather than inventing
its own structure.

### Category 2: Specificity and Information

These principles ensure the model has enough detail to produce
accurate output.

**Principle: Use few-shot prompting (example-driven)**

Provide 1-3 examples of the desired input-output pair before giving
the actual task. This is the single most effective technique for
consistent output.

```
Convert the following method names to test names.

Example 1:
  Method: createPerson
  Test: createPerson_validInput_returnsPerson

Example 2:
  Method: findById
  Test: findById_existingId_returnsPerson

Example 3:
  Method: findById
  Test: findById_nonExistentId_throwsEntityNotFoundException

Now convert:
  Method: updatePerson
  Method: deletePerson
```

Few-shot examples are especially powerful inside SKILL.md files
(see Section 9) -- they show the AI exactly what output format you expect.

**Principle: State explicit requirements**

Use direct phrasing like "Your task is" and "You MUST" to make
requirements unambiguous.

```
Your task is to generate a MapStruct mapper for the Person entity.

You MUST:
- Use @Mapper(componentModel = "spring")
- Map all fields explicitly (no implicit mapping)
- Handle null source objects gracefully
- Include an inverse mapping method
```

**Principle: Ask for detailed output when needed**

If you need comprehensive output, say so explicitly.

```
Write a detailed analysis of the PersonService class including
all methods, their transaction boundaries, potential thread-safety
issues, and suggestions for improvement. Add all necessary details.
```

### Category 3: User Interaction and Engagement

These principles leverage the conversational nature of LLMs.

**Principle: Let the model ask questions first**

For complex tasks, allow the model to clarify requirements before
producing code. This prevents wasted iterations.

```
I need to add a new feature to the Person service. Before writing
any code, ask me questions about the requirements until you have
enough information to implement it correctly. Ask about: the use
case, validation rules, error handling, and testing expectations.
```

This is particularly useful for open-ended tasks like "add search
functionality" or "implement export feature" where the AI needs
domain context it cannot infer.

### Category 4: Content and Language Style

These principles shape how the model communicates.

**Principle: Assign a role**

Telling the model to adopt a specific persona changes the expertise
and vocabulary applied to the response.

```
You are a senior Java backend engineer specializing in Spring Boot
and distributed systems. You write production-grade code following
Clean Code principles and always consider thread safety.
```

In AGENTS.md, this translates to the Project Overview section that
establishes the project's technology context. In SKILL.md, it
becomes the opening instruction that frames the skill's behavior.

**Principle: Be direct -- skip pleasantries**

Polite filler ("Could you please...", "If you don't mind...")
wastes tokens without improving output quality. Be direct.

```
# Wastes tokens
Could you please help me write a service method that creates a
person? If it's not too much trouble, please also add validation.
Thank you in advance!

# Direct
Write a PersonService.create() method that:
1. Validates the input DTO
2. Maps DTO to entity
3. Saves via repository
4. Returns the response DTO
```

**Principle: Repeat critical constraints**

When a rule is critical, repeat it. The model weights repeated
information higher.

```
Generate a REST controller for Person CRUD operations.
Use Java records for all DTOs.
Each endpoint MUST return a DTO record, never an entity.
The response type is always a record.
```

### Category 5: Complex Tasks and Coding

These principles handle multi-step and programming-specific tasks.

**Principle: Break complex tasks into sequential subtasks**

Instead of asking for an entire feature at once, decompose it into
a chain of focused prompts. Each prompt builds on the previous
output.

```
Step 1: "Design the database schema for an Order entity with
         line items. Show the Liquibase changelog."

Step 2: "Based on the schema above, generate the Java entity
         classes using Spring Data JDBC."

Step 3: "Now create the Repository interface with custom query
         methods for finding orders by customer and date range."

Step 4: "Create the Service layer with create, update, and
         cancel operations. Include @Transactional boundaries."

Step 5: "Finally, create the RestController with OpenAPI
         annotations. Use the DTOs from step 2."
```

This approach produces better results than a single "create the
entire Order feature" prompt because each step has focused context.

**Principle: Combine Chain-of-Thought with few-shot**

For reasoning-heavy tasks (debugging, optimization, architecture
decisions), combine step-by-step reasoning with examples.

```
Analyze the following Spring Boot endpoint for performance issues.
Think through each layer step by step.

Example analysis:
  Layer: Controller
  Issue: @RequestBody deserialized twice due to logging interceptor
  Impact: 2x memory allocation per request
  Fix: Use ContentCachingRequestWrapper

Now analyze this endpoint:
@PostMapping("/persons")
public ResponseEntity<PersonResponse> create(@RequestBody CreatePersonRequest request) {
    log.info("Request body: {}", request);
    // ...
}
```

**Principle: Multi-file generation pattern**

When the task spans multiple files, instruct the model to produce
them in dependency order with clear file boundaries.

```
Generate the following files for a new Order feature. Produce them
in this exact order, with each file clearly marked:

1. `dto/CreateOrderRequest.java`
2. `dto/OrderResponse.java`
3. `domain/Order.java`
4. `mapper/OrderMapper.java`
5. `repository/OrderRepository.java`
6. `service/OrderService.java`
7. `service/impl/OrderServiceImpl.java`
8. `controller/OrderController.java`

For each file, start with the full package declaration and imports.
```

---

## Applying Prompting Techniques to Assistant Configuration

The techniques above are not just for chat -- they directly improve the
quality of your `AGENTS.md`, skills, and commands.

### In AGENTS.md (Section 8)
- **Audience**: State the target persona ("This project is developed by
  senior Java engineers...")
- **Affirmative directives**: Write rules as "do X" not "don't do Y"
- **Delimiters**: Use clear markdown sections with headers
- **Explicit requirements**: Use "MUST" and "NEVER" for hard rules

### In SKILL.md (Section 9)
- **Few-shot examples**: Include 1-2 input/output examples in the body
- **Role assignment**: Open with "You are a ... specialist"
- **Output format**: Specify the exact response structure
- **Explicit requirements**: Use "Your task is" and "You MUST"

### In Custom Commands (Section 10)
- **Chain-of-thought**: Add "think step by step" for review/analysis commands
- **Output primers**: End command instructions with the start of the
  expected output
- **Task decomposition**: Break complex commands into numbered steps
- **Role assignment**: Open with the persona the command should adopt

---

## Practical Examples: Each Principle Applied

This section provides a full-context, realistic example for every
principle discussed above. Each example shows a concrete Java/Spring
scenario so you can copy and adapt the pattern.

### 1. Audience (Principle 2)

**Scenario:** You want the AI to explain a Spring Data JDBC mapping.

```
Without audience:
  "Explain AggregateReference in Spring Data JDBC."

With audience (junior developer):
  "Explain AggregateReference in Spring Data JDBC.
   The audience is a junior Java developer who understands JPA
   @ManyToOne but has never used Spring Data JDBC."

With audience (architect):
  "Explain AggregateReference in Spring Data JDBC.
   The audience is a software architect evaluating whether to
   migrate from JPA to Spring Data JDBC for a high-throughput
   service handling 10K requests/sec."
```

The junior version will get a step-by-step explanation with code
samples. The architect version will get a trade-off analysis with
performance implications.

### 2. Affirmative Directives (Principle 4)

**Scenario:** Writing architecture rules in AGENTS.md.

```markdown
# Negative version (less reliable)
## Architecture Rules
- Controllers CANNOT inject Repositories
- Don't use wildcard imports
- Do NOT return entities from REST endpoints
- Never put @Transactional on controllers
- Avoid using Optional.get() without isPresent()

# Affirmative version (more reliable)
## Architecture Rules
- Controllers inject Service interfaces only
- Use explicit imports for every class
- Return DTO records from all REST endpoints
- Place @Transactional on Service implementation methods only
- Use Optional.orElseThrow() with a descriptive exception
```

### 3. Delimiters (Principle 17)

**Scenario:** Asking the AI to refactor a service method.

```
###Instruction###
Refactor the following method to use Java 21 pattern matching
and extract the validation logic into a private method.

###Current Code###
public PersonResponse updatePerson(Long id, UpdatePersonRequest request) {
    Person person = repository.findById(id).orElse(null);
    if (person == null) {
        throw new EntityNotFoundException("Person not found: " + id);
    }
    if (request.firstName() == null || request.firstName().isBlank()) {
        throw new ValidationException("firstName is required");
    }
    if (request.lastName() == null || request.lastName().isBlank()) {
        throw new ValidationException("lastName is required");
    }
    person.setFirstName(request.firstName());
    person.setLastName(request.lastName());
    Person saved = repository.save(person);
    return mapper.toResponse(saved);
}

###Constraints###
- Keep the method signature unchanged
- Use orElseThrow() instead of orElse(null)
- Extract validation into: private void validate(UpdatePersonRequest)
- Maintain existing exception types

###Output###
Show only the refactored code, no explanations.
```

### 4. Chain-of-Thought (Principle 12)

**Scenario:** Debugging a failing integration test.

```
The following integration test is failing with
"expected 200 but got 400". Think step by step:

1. Read the test setup -- what data is being prepared?
2. Read the request being sent -- does it match the
   controller's @Valid constraints?
3. Check the DTO record -- does the compact canonical
   constructor impose additional validation?
4. Check the service layer -- are there business rule
   validations that could reject the input?
5. After analyzing all layers, explain the root cause
   and provide the fix.

@Test
void createPerson_validInput_returns200() {
    var request = new CreatePersonRequest("", "Smith", Sex.MALE);
    mockMvc.perform(post("/api/persons")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
}
```

Without CoT, the model might guess. With CoT, it will trace through
the layers and identify that the empty `firstName` triggers the
`@NotBlank` validation (or the compact constructor check).

### 5. Output Primers (Principle 20)

**Scenario:** Generating a Spring configuration class.

```
Generate a Spring Boot configuration class that sets up a
RestTemplate with connection pooling, timeouts, and error
handling for calling an external API.

Package: com.example.myservice.config

@Slf4j
@Configuration
public class RestTemplateConfig {
```

The model will continue from the provided class stub, producing
code that matches your package structure and annotation style.

### 6. Few-Shot Prompting (Principle 7)

**Scenario:** Generating exception handler methods.

```
Generate @ExceptionHandler methods for the given exception types.
Follow the pattern shown in the examples below.

Example 1:
  Exception: EntityNotFoundException
  Handler:
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }

Example 2:
  Exception: ValidationException
  Handler:
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleValidation(ValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return new ErrorResponse("VALIDATION_ERROR", ex.getMessage());
    }

Now generate handlers for:
  - ConflictException (409)
  - RateLimitException (429)
  - ExternalServiceException (502)
```

The model will mirror the logging pattern, return type, and
annotation style from the examples.

### 7. Explicit Requirements (Principle 9)

**Scenario:** Generating a repository interface.

```
Your task is to create a PersonRepository interface.

You MUST:
- Extend CrudRepository<Person, Long>
- Annotate with @Repository
- Include findByEmail(String email) returning Optional<Person>
- Include findAllByStatus(PersonStatus status) returning List<Person>
- Include a @Query method for full-text search on firstName and lastName
- Use named parameters (:param) in all @Query annotations
- Add Javadoc on every method

You MUST NOT:
- Use native queries
- Return raw entity lists without Optional wrapping for single results
- Add pagination (it will be added in a separate PR)
```

### 8. Asking for Questions First (Principle 14)

**Scenario:** Implementing a notification feature.

```
I need to add email notifications when a person's status changes.
Before writing any code, ask me questions to clarify the requirements.

Ask about:
- Which status transitions trigger notifications?
- Should notifications be synchronous or async (Spring Events)?
- What email provider are we using (SMTP, SendGrid, SES)?
- Should we store notification history in the database?
- What should happen if the email fails to send?
- Are there any rate limiting requirements?
- Should the email template be externalized?

Only start implementing after I answer all your questions.
```

### 9. Role Assignment (Principle 16)

**Scenario:** Different roles produce different results for the
same code review request.

```
# Security reviewer
You are a senior application security engineer specializing in
OWASP Top 10 vulnerabilities in Java/Spring applications.
Review PersonController.java for security issues only.

# Performance reviewer
You are a performance engineer specializing in JVM tuning and
Spring Boot optimization. Review PersonController.java for
performance bottlenecks, memory allocation patterns, and
opportunities to reduce latency.

# Architecture reviewer
You are a software architect enforcing Clean Architecture
principles. Review PersonController.java for layer violations,
coupling issues, and SOLID principle adherence.
```

Each role produces a fundamentally different review even when
given the same file.

### 10. Be Direct (Principle 1)

**Scenario:** Requesting a mapper class.

```
# Indirect (wastes tokens)
Hi! Could you please help me create a mapper? I was wondering if
you could use MapStruct for it. If it's not too much trouble, could
you also handle the null case? That would be really helpful. Thanks
so much in advance!

# Direct (same request, fewer tokens, same quality)
Create a MapStruct mapper for Person <-> PersonResponse.
Use @Mapper(componentModel = "spring").
Handle null source by returning null.
Include toResponse(Person) and toEntity(CreatePersonRequest).
```

### 11. Repeat Critical Constraints (Principle 18)

**Scenario:** The model keeps returning entities instead of DTOs.

```
Generate CRUD endpoints for the Order entity.

IMPORTANT: Every endpoint returns an OrderResponse record.
The controller MUST NOT return Order entities.
All response types are DTO records.
Do not expose entity classes through the REST API.
Response type: OrderResponse (record), never Order (entity).
```

Repeating the constraint in different phrasings makes it much
harder for the model to ignore.

### 12. Task Decomposition (Principle 3)

**Scenario:** Adding a search feature with filtering, pagination,
and sorting across the full stack.

Instead of one massive prompt, use a chain:

```
Prompt 1 (Schema):
"Add a database index for searching persons by first_name,
 last_name, and email. Produce a Liquibase changelog."

Prompt 2 (Repository):
"Add a search method to PersonRepository that accepts optional
 firstName, lastName, and email filters with pagination.
 Use Spring Data JDBC @Query with dynamic WHERE clauses."

Prompt 3 (Service):
"Create a PersonSearchService that accepts a SearchPersonRequest
 record, delegates to the repository search method, and returns
 Page<PersonResponse>."

Prompt 4 (Controller):
"Add GET /api/persons/search to PersonController. Accept query
 parameters: firstName, lastName, email, page, size, sort.
 Delegate to PersonSearchService. Use OpenAPI @Parameter annotations."

Prompt 5 (Test):
"Write integration tests for the search endpoint covering:
 no filters (returns all), single filter, combined filters,
 pagination, and empty results."
```

### 13. CoT + Few-Shot Combined (Principle 19)

**Scenario:** Analyzing a complex SQL query for optimization.

```
Analyze the following SQL query for optimization opportunities.
Think step by step through each aspect, following the example format.

Example:
  Query: SELECT * FROM orders WHERE customer_id = 123
  Step 1 (Selectivity): SELECT * fetches all columns -- wasteful
  Step 2 (Indexing): customer_id likely needs an index
  Step 3 (Result): Replace * with needed columns; add index on customer_id

Now analyze:
  SELECT p.*, o.*, oi.*
  FROM person p
  LEFT JOIN orders o ON o.person_id = p.id
  LEFT JOIN order_items oi ON oi.order_id = o.id
  WHERE p.status = 'ACTIVE'
  ORDER BY o.created_at DESC
```

### 14. Multi-File Generation (Principle 23)

**Scenario:** Scaffolding a complete audit logging feature.

```
Generate the following files for an audit logging feature.
Produce each file in order with the full package declaration
and all imports. Mark each file with its relative path.

1. `domain/AuditEvent.java` -- entity with: id, entityType,
   entityId, action (enum: CREATE/UPDATE/DELETE), payload (JSON),
   performedBy, performedAt

2. `dto/AuditEventResponse.java` -- response record

3. `repository/AuditEventRepository.java` -- with findByEntityTypeAndEntityId
   and findByPerformedAtBetween

4. `service/AuditService.java` -- interface with log() and query() methods

5. `service/impl/AuditServiceImpl.java` -- implementation using
   @Async for non-blocking audit writes

6. `aspect/AuditAspect.java` -- AOP aspect using @AfterReturning
   on all @Service methods annotated with @Auditable

7. `annotation/Auditable.java` -- custom annotation with
   entityType parameter

8. `controller/AuditController.java` -- GET /api/audit with
   filters for entityType, dateRange, performedBy

Each file MUST use Java 21 features where applicable (records,
pattern matching, text blocks for any multi-line strings).
```

---

## Quick-Reference: Principle Checklist

Use this checklist before writing or reviewing any prompt, skill,
or command:

| # | Checkpoint | Principle |
|---|---|---|
| 1 | Did I assign a role/persona? | Principle 16 |
| 2 | Did I state the task with "Your task is"? | Principle 9 |
| 3 | Did I specify the audience? | Principle 2 |
| 4 | Did I use affirmative language ("do X")? | Principle 4 |
| 5 | Did I include examples (few-shot)? | Principle 7 |
| 6 | Did I separate sections with delimiters? | Principle 17 |
| 7 | Did I add "think step by step" for complex reasoning? | Principle 12 |
| 8 | Did I specify the output format? | Principle 8 |
| 9 | Did I break the task into subtasks if complex? | Principle 3 |
| 10 | Did I use explicit requirements ("MUST", "NEVER")? | Principle 25 |

---

## References

1. Bsharat, S. M., Myrzakhan, A., & Shen, Z. (2024). *Principled
   Instructions Are All You Need for Questioning LLaMA-1/2, GPT-3.5/4*.
   [arXiv:2312.16171](https://arxiv.org/pdf/2312.16171)

2. Damji, J. S. (2024). *Best Prompt Techniques for Best LLM Responses*.
   [Medium / The Modern Scientist](https://medium.com/the-modern-scientist/best-04-prompt-techniques.md-for-best-llm-responses-24d2ff4f6bca)

3. Teo, S. (2024). *How I Won Singapore's GPT-4 Prompt Engineering
   Competition*. Towards Data Science.

4. Saravia, E. *Prompt Engineering Guide*.
   [promptingguide.ai](https://www.promptingguide.ai/)

5. OpenAI. *Prompt Engineering Guide*.
   [platform.openai.com/docs/guides/prompt-engineering](https://platform.openai.com/docs/guides/prompt-engineering)

## Prompting Anti-Patterns for Java Developers

Even experienced developers fall into prompting traps that produce mediocre
output. Recognizing these anti-patterns is as important as learning the good
techniques. Each pattern below shows a real before/after transformation.

### Anti-Pattern 1: Vague Task Description

The most common mistake. Vague prompts force the model to guess your intent,
architecture, and conventions.

```
❌ BAD:
Make this service better.

✅ GOOD:
Refactor PersonService to use Java records for all DTOs, keeping
backward compatibility with existing JSON contracts. Replace the
mutable PersonDto class with an immutable CreatePersonRequest record
and a PersonResponse record. Ensure Jackson deserialization still
works for clients sending the old field names by adding
@JsonProperty aliases where needed.
```

Why it matters: "better" is subjective. The model might improve naming,
add logging, rewrite algorithms, or do something you never intended.
A specific task produces a specific result.

### Anti-Pattern 2: Missing Architecture Context

Asking the AI to create a component without specifying where it fits in
your architecture leads to code that violates your layer boundaries.

```
❌ BAD:
Create a service that sends email notifications when a person is updated.

✅ GOOD:
Create a PersonNotificationService in the com.example.myservice.service
package. Our architecture uses layered design:
- Controllers → Services → Repositories (no skipping layers)
- Services inject other Service interfaces, never Repositories of
  other aggregates
- Async operations use Spring @Async with a custom TaskExecutor

The PersonNotificationService should:
1. Be triggered by PersonService.update() via a Spring ApplicationEvent
2. Send emails asynchronously using the existing EmailGateway interface
3. Not block the update transaction
```

Why it matters: without architecture context, the model might inject
the `PersonRepository` directly, use `@Autowired` field injection, or
put business logic in the wrong layer.

### Anti-Pattern 3: Overloading a Single Prompt

Asking for everything in one go overwhelms the model's attention and
produces inconsistencies across the generated files.

```
❌ BAD:
Generate the Person entity, CreatePersonRequest DTO, PersonResponse DTO,
PersonMapper using MapStruct, PersonRepository, PersonService interface,
PersonServiceImpl, PersonController with OpenAPI annotations, and unit
tests for the service with at least 80% coverage. Use Java 21 records,
Spring Data JDBC, and PostgreSQL.

✅ GOOD (decompose into a chain):
Prompt 1: "Generate the CreatePersonRequest and PersonResponse records
           with Jakarta validation annotations."
Prompt 2: "Based on the DTOs above, create the Person entity for
           Spring Data JDBC with @Table annotation."
Prompt 3: "Create the PersonMapper using MapStruct that converts between
           the entity and the DTOs from the previous step."
Prompt 4: "Create the PersonRepository extending CrudRepository<Person, Long>."
Prompt 5: "Create PersonService interface and PersonServiceImpl that uses
           the mapper and repository from previous steps."
Prompt 6: "Create PersonController with OpenAPI annotations that delegates
           to PersonService."
Prompt 7: "Write unit tests for PersonServiceImpl using the BDD
           given/when/then structure."
```

Why it matters: the model's output quality degrades as prompt complexity
increases. Each focused prompt gets the model's full attention. See
Section 3's Least-to-Most technique for the formal approach.

### Anti-Pattern 4: Ignoring the Output Format

Not specifying the test framework, assertion library, or structural
expectations leads to output you have to manually reformat.

```
❌ BAD:
Generate tests for PersonService.

✅ GOOD:
Generate JUnit 5 tests for PersonService using:
- @ExtendWith(MockitoExtension.class) for mocking
- AssertJ assertions (assertThat, assertThatThrownBy)
- @DisplayName with human-readable descriptions
- BDD structure: // given, // when, // then sections
- Naming convention: methodName_stateUnderTest_expectedBehavior

Cover these scenarios for the create() method:
1. Valid input → returns PersonResponse with generated id
2. Null request → throws IllegalArgumentException
3. Duplicate email → throws ConflictException
```

Why it matters: "generate tests" might produce TestNG tests with
Hamcrest matchers in a flat structure with auto-generated names.
Specifying the format means the output is merge-ready.

### Anti-Pattern 5: Not Providing Error Context

Pasting an error message without the stack trace, relevant code, or
environment context makes the model guess at the root cause.

```
❌ BAD:
I'm getting a NullPointerException. Fix it.

✅ GOOD:
I'm getting a NullPointerException on line 42 of PersonServiceImpl.java
when calling personMapper.toResponse(saved).

Stack trace:
  java.lang.NullPointerException: Cannot invoke "PersonMapper.toResponse(Person)"
  because "this.personMapper" is null
  at com.example.myservice.service.impl.PersonServiceImpl.create(PersonServiceImpl.java:42)

Relevant code:
  @Service
  public class PersonServiceImpl implements PersonService {
      private final PersonRepository repository;
      private PersonMapper personMapper;  // <-- not injected

      public PersonServiceImpl(PersonRepository repository) {
          this.repository = repository;
      }
  }

Environment: Spring Boot 3.5, Java 21, MapStruct 1.5.5, using
constructor injection. The mapper is annotated with
@Mapper(componentModel = "spring").
```

Why it matters: with full context, the model immediately identifies
that `personMapper` is declared but not injected through the constructor.
Without context, it might suggest adding a null check instead of fixing
the injection.

### Anti-Pattern 6: Asking for Opinions Instead of Decisions

Open-ended questions produce open-ended answers. Decision-framed
questions produce actionable recommendations.

```
❌ BAD:
What do you think about our error handling approach?

✅ GOOD:
Compare these two error-handling strategies for our Spring Boot REST API
and recommend one with rationale:

Option A: @ControllerAdvice with @ExceptionHandler methods that return
ErrorResponse records, one handler per exception type.

Option B: A single HandlerExceptionResolver that maps exception classes
to HTTP status codes via a configuration Map<Class, HttpStatus>.

Evaluation criteria:
1. Maintainability as exception types grow from 5 to 20
2. Testability in isolation
3. Consistency of error response format
4. Spring Boot 3.5 best practices

Recommend Option A or B (or a hybrid) with a concrete rationale for each
criterion.
```

Why it matters: "what do you think" invites a wishy-washy discussion.
Structured comparison with explicit criteria produces a decision you
can act on.

---

## Quick-Reference Cheat Sheet: CO-STAR for Java Developers

Use this table as a pre-flight checklist before writing any prompt.
Each component has a one-line Java-specific example.

| CO-STAR Element | Question | Java Example |
|---|---|---|
| **C** -- Context | What does the model need to know about the project? | `Spring Boot 3.5, Java 21, Spring Data JDBC, PostgreSQL, layered architecture` |
| **O** -- Objective | What specific task should it perform? | `Create a paginated search endpoint for Person with filtering by name and email` |
| **S** -- Style | What coding style and patterns to follow? | `Use records for DTOs, constructor injection, @Transactional on service layer only` |
| **T** -- Tone | What quality level and completeness? | `Production-quality code, no TODOs, no placeholders, include Javadoc on public methods` |
| **A** -- Audience | Who will consume this output? | `Senior Java developers performing merge-request review` |
| **R** -- Response | What exact format and structure? | `Produce files in order: DTO records → Service interface → ServiceImpl → Controller` |

**Template you can copy and fill in:**

```
Context: [tech stack, project structure, relevant existing code]

Objective: [specific task -- "Create...", "Refactor...", "Debug..."]

Style: [architecture rules, coding conventions, patterns to follow]

Tone: [production-quality | prototype | learning exercise]

Audience: [who reads the output -- reviewers, juniors, CI pipeline]

Response: [file format, ordering, structure, what to include/exclude]
```

---

---

## Multi-Project Prompt Management

Sections 8–10 cover AGENTS.md, skills, and custom commands for a single project.
[Section 7: Prompt Optimization](07-prompt-optimization.md) covers debugging and
testing those prompts. This part addresses **managing prompts across multiple
projects** — when copying the same code-review skill into five services guarantees
drift within a month.

### 1. Storage Layers

Prompts live at three distinct levels, each with different scope, ownership,
and tooling:

```text
┌────────────────────────────────────────────────────────────────────┐
│  GLOBAL       ~/.agents/skills     ~/.cursor/rules/              │
│               Personal defaults. Applied to every project on      │
│               this machine. Managed by chezmoi or GNU Stow.       │
├────────────────────────────────────────────────────────────────────┤
│  TEAM         git.company.com/team/prompt-library                 │
│               Shared conventions. Consumed as a Git submodule,    │
│               symlink, or copied during CI. Owned by the team.    │
├────────────────────────────────────────────────────────────────────┤
│  PROJECT      AGENTS.md   skills   .cursor/commands/             │
│               Project-specific rules. Already covered in          │
│               Sections 8–10. Committed alongside the source code.  │
└────────────────────────────────────────────────────────────────────┘
```

### When to use each layer

| Layer     | Content                                          | Lifespan       |
|-----------|--------------------------------------------------|----------------|
| **Global**  | Personal coding style, preferred review format, shell snippets | Permanent (follows you across projects) |
| **Team**    | Architecture conventions, testing patterns, migration templates | Evolves with the team's standards |
| **Project** | Domain-specific rules, entity conventions, CI pipeline details | Lives and dies with the project |

**The rule of thumb:** if you find yourself copying the same prompt into
a third project, it belongs one layer higher.

### How layers compose

When an AI assistant processes a request, it sees all three layers
simultaneously. The layering creates an inheritance chain:

```text
Global defaults (broadest, lowest priority)
  └── Team conventions (narrower, higher priority)
        └── Project rules (most specific, highest priority)
```

A project-level AGENTS.md can override a team-level convention just like
a subclass overrides a parent method. The more specific layer wins.

---

### 2. Multi-Project Prompt Management Strategies

### 2.1 Dedicated Git Repository

The most straightforward approach: store all shared prompts in a single
Git repository that every project references.

```text
prompt-library/                        ← standalone repo
├── README.md
├── global/                            ← chezmoi-managed, linked to ~/.agents/skills
│   ├── code-review/SKILL.md
│   └── generate-tests/SKILL.md
├── teams/
│   ├── backend/
│   │   ├── java-spring/SKILL.md
│   │   └── db-migration/SKILL.md
│   └── DevOps
│       └── k8s-deploy/SKILL.md
├── commands/                          ← shared slash commands
│   ├── review.md
│   └── migration.md
└── tests/                             ← promptfoo golden sets (Section 7)
    └── code-review.yaml
```

Each skill follows the same `SKILL.md` format from Section 9. The
`tests/` directory uses promptfoo configurations from Section 7 to run
golden test sets against the shared prompts.

**Advantages:** single source of truth, full Git history, pull request
reviews on prompt changes.

**Trade-off:** requires discipline to keep the library updated; projects
must actively pull changes.

### 2.2 Git Submodules

Pin an exact version of the prompt library inside each project:

```bash
git submodule add git@git.company.com:team/prompt-library.git skills/shared
```

This creates a `skills/shared/` directory in the project that points to a
specific commit of the prompt library. The project controls when to update:

```bash
cd skills/shared
git pull origin main
cd ../..
git add skills/shared
git commit -m "chore: update shared skills to v2.3.0"
```

**Advantages:** reproducible builds -- every project pins a known version.
No surprises from upstream changes.

**Trade-off:** requires explicit update steps. Easy to forget and fall
behind.

### 2.3 GNU Stow (Symlink Farm)

GNU Stow creates a tree of symbolic links from a source directory to a
target directory. It is ideal for activating shared skills globally:

```bash
# Clone the prompt library
git clone git@git.company.com:team/prompt-library.git ~/prompt-library

# Link the global skills into ~/.agents/skills
stow -d ~/prompt-library -t ~/.agents/skills global
```

After running `stow`, each skill directory under `~/prompt-library/global/`
appears as a symlink under `~/.agents/skills`. Editing the file in
either location edits the same underlying file.

**Advantages:** zero copying, instant updates, easy to reverse
(`stow -D` removes all links).

**Trade-off:** symlinks can confuse some tools; only works on a single
machine (not a deployment mechanism).

### 2.4 Chezmoi (Dotfile Manager)

Chezmoi is a dotfile manager designed for configuration files that need
to be synchronized across multiple machines. It handles templates,
encryption, and machine-specific variants.

```bash
# Initialize chezmoi with your prompt library repo
chezmoi init git@git.company.com:you/dotfiles.git

# Add your global skills to chezmoi's management
chezmoi add ~/.agents/skills/code-review/SKILL.md
chezmoi add ~/.cursor/rules/java-conventions.md

# On a new machine, apply everything
chezmoi apply
```

Chezmoi supports templating, so you can have machine-specific variants:

```markdown
# SKILL.md template managed by chezmoi
You are a senior {{ .team }} engineer specializing in
{{ .framework }} applications.

{{ if eq .env "production" -}}
NEVER generate debug logging or System.out.println.
{{ end -}}
```

**Advantages:** handles multiple machines, supports encryption for
sensitive prompts (API keys in examples), template variables for
per-machine customization.

**Trade-off:** requires learning chezmoi's conventions; more setup than
Stow for simple use cases.

### 2.5 Direnv (Per-Directory Environment Switching)

Direnv loads and unloads environment variables when you enter and leave
directories. Use it to point `AGENTS_SKILLS_PATH` at different prompt
libraries for different project groups:

```bash
# ~/work/backend/.envrc
export AGENTS_SKILLS_PATH="$HOME/prompt-library/teams/backend"

# ~/work/DevOps.envrc
export AGENTS_SKILLS_PATH="$HOME/prompt-library/teams/DevOps"
```

When you `cd` into a project under `~/work/backend/`, direnv
automatically activates the backend skill set. Leave the directory, and
the variable is unloaded.

**Advantages:** zero-config switching between prompt sets; works with
any tool that reads environment variables.

**Trade-off:** not all AI assistants read `AGENTS_SKILLS_PATH`; may
require custom shell integration for some tools.

### 2.6 Combining Strategies

In practice, most teams combine two or three approaches:

```
Chezmoi         → sync personal global skills across machines
Git submodule   → pin team skills at a known version per project
Direnv          → switch between team/personal skill sets by directory
```

There is no single correct combination. Start with the dedicated repo
(Section 2.1) and add linking mechanisms as your needs grow.

---

### 3. Prompt Versioning

### Why Version Prompts

A prompt that works with Claude Sonnet 3.5 may need adjustments for
Claude Sonnet 4 or GPT-4.1. Without version tracking, you cannot:

- Roll back a prompt change that broke output quality
- Know which model a prompt was last tested against
- Reproduce an issue a colleague reported last week
- Audit what changed and why

### Version Strategy

**Keep prompts in Git.** Each skill is a file. Each change is a commit.
This is the minimum viable versioning strategy and it works well for
most teams.

**Tag releases.** When the team agrees a prompt set is stable:

```bash
git tag -a v1.2.0 -m "Tested against Claude Sonnet 3.7 and GPT-4o"
```

Projects using submodules can pin to this tag instead of a moving branch.

**Branch strategy:**
- `main` -- stable, tested prompts
- `experimental` -- new techniques under evaluation

Never deploy an `experimental` prompt to all projects without testing it
through a golden test set first (Section 7).

### Front-Matter Metadata

Add YAML front-matter to each SKILL.md for machine-readable metadata:

```yaml
---
version: "2.1.0"
tested_with:
  - "claude-sonnet-4-20250514"
  - "gpt-4o-2025-03-26"
changed: "2026-04-10"
author: "backend-team"
tags:
  - "java"
  - "spring-boot"
  - "testing"
---
```

This metadata serves two purposes:

1. **Humans** can quickly see when a skill was last updated and which
   models it supports.
2. **Automation** (promptfoo, CI scripts) can filter and run tests against
   the declared model versions.

### Changelog Comments

For inline tracking without front-matter, use HTML comments:

```markdown
## Testing Conventions
<!-- v3 - 2026-04-10: switched from @Mock to @MockitoBean -->
<!-- v2 - 2026-03-01: added ObjectMother pattern -->
<!-- v1 - 2026-01-15: initial testing conventions -->

MUST use @MockitoBean for Spring-managed beans in @WebMvcTest classes.
```

---

### 4. Linux Tools for Prompt Management

This section covers tools that are available on Linux (most also work on
macOS) and make managing a prompt library practical at the terminal level.

### 4.1 Core File Management

| Tool          | Purpose                                          | Install                  |
|---------------|--------------------------------------------------|--------------------------|
| **chezmoi**   | Dotfile sync across machines; templates, encryption | `apt install chezmoi` or `sh -c "$(curl -fsLS get.chezmoi.io)"` |
| **GNU Stow**  | Symlink farm for linking prompt libraries        | `apt install stow`       |
| **direnv**    | Per-directory environment variables              | `apt install direnv`     |

### 4.2 Search and Navigation

| Tool          | Purpose                                          | Install                  |
|---------------|--------------------------------------------------|--------------------------|
| **fzf**       | Fuzzy-search your prompt library interactively   | `apt install fzf`        |
| **fd**        | Fast file finder (better `find`)                 | `apt install fd-find`    |
| **ripgrep**   | Fast content search across prompt files          | `apt install ripgrep`    |
| **zoxide**    | Frecency-based `cd` -- jump to prompt repos fast | `apt install zoxide`     |

### 4.3 Knowledge Base / Notes Applications

| Tool          | Purpose                                          | Install                  |
|---------------|--------------------------------------------------|--------------------------|
| **Obsidian**  | Markdown knowledge base with graph view; tag and link prompts, results, and notes | AppImage from obsidian.md |
| **Logseq**    | Graph-based knowledge management; outliner interface; good for prompt relationships | AppImage from logseq.com |
| **Joplin**    | Open-source Markdown notes with E2EE sync; lighter than Obsidian | Snap or AppImage |
| **Zettlr**    | Academic-style Markdown editor with Zettelkasten support | `.deb` from zettlr.com |

### 4.4 Structured Storage

| Tool              | Purpose                                      | Install                  |
|-------------------|----------------------------------------------|--------------------------|
| **SQLite + datasette** | Structured prompt library with metadata (model, version, tags); browsable web UI | `pip install datasette`  |
| **promptfoo**     | Prompt regression testing (covered in Section 7) | `npm install -g promptfoo` |

### Choosing the Right Combination

Most developers need surprisingly few tools. A practical starting set:

```
Minimum:   Git + fzf + fd
Standard:  Git + chezmoi + fzf + fd + direnv
Full:      Git + chezmoi + fzf + fd + direnv + Obsidian + promptfoo
```

Do not install everything at once. Start with `Git + fzf + fd`, and add
tools when you feel a specific pain point.

---

### 5. Shell Workflow Snippets

These functions and aliases make daily prompt management fast. Add them
to your `~/.zshrc` or `~/.bashrc`.

### 5.1 Fuzzy-Pick a Skill (`fp`)

Interactively search your prompt library, preview the selected skill,
and copy it into the current project:

```bash
fp() {
    local lib="${PROMPT_LIBRARY:-$HOME/prompt-library}"
    local skill
    skill=$(fd SKILL.md "$lib" | fzf --preview 'head -40 {}' --preview-window=right:60%)
    [ -z "$skill" ] && return 1

    local dir
    dir=$(dirname "$skill")
    local name
    name=$(basename "$dir")
    local target="skills$name"

    mkdir -p "$target"
    cp "$skill" "$target/SKILL.md"
    echo "Copied $name skill to $target/SKILL.md"
}
```

Usage:

```bash
cd ~/work/my-service
fp
# → fuzzy-pick "code-review" → copies to skills/code-review/SKILL.md
```

### 5.2 Activate Global Skills with Stow

```bash
alias stow-skills='stow -d ~/prompt-library -t ~/.agents/skills global'
alias unstow-skills='stow -D -d ~/prompt-library -t ~/.agents/skills global'
```

### 5.3 Chezmoi Apply on a New Machine

```bash
# One-liner to bootstrap a new machine with all prompts
chezmoi init --apply git@git.company.com:you/dotfiles.git
```

### 5.4 Search All Prompts for a Keyword

```bash
alias rg-prompts='rg --type md --glob "SKILL.md" --glob "AGENTS.md"'
```

Usage:

```bash
rg-prompts "@Transactional" ~/prompt-library
```

### 5.5 List Skills with Metadata

Extract version and tested_with from front-matter across all skills:

```bash
skill-versions() {
    local lib="${PROMPT_LIBRARY:-$HOME/prompt-library}"
    fd SKILL.md "$lib" -x sh -c '
        printf "%-40s " "$(dirname {} | xargs basename)"
        grep -m1 "^version:" {} 2>/dev/null || echo "(no version)"
    '
}
```

---

### 6. Prompt Library Design Patterns

### 6.1 Base + Override Pattern

Define a base skill with common conventions and let projects override
specific sections:

```text
prompt-library/
└── teams/backend/
    └── code-review/
        ├── SKILL.md              ← base review skill
        └── overrides/
            ├── strict.md         ← stricter rules for payment services
            └── relaxed.md        ← lighter rules for internal tools
```

The project's AGENTS.md selects the variant:

```markdown
## Code Review
Use the `code-review` skill with the `strict` override.
```

### 6.2 Composable Skills

Break large skills into small, composable units that can be combined:

```text
prompt-library/
└── global/
    ├── review-architecture/SKILL.md
    ├── review-security/SKILL.md
    ├── review-performance/SKILL.md
    └── review-style/SKILL.md
```

A project can reference specific review aspects in its commands:

```markdown
# /review command
Apply the following skills in order:
1. review-architecture
2. review-security
3. review-style
Skip review-performance for this project.
```

This is analogous to composing small, focused functions instead of
writing one monolithic method.

### 6.3 Template Skills

Use placeholder tokens that the consuming project fills in:

```markdown
# SKILL.md (template)
You are a senior {LANGUAGE} engineer specializing in {FRAMEWORK}.

Review the given code for:
- Layer violations per the {ARCHITECTURE} pattern
- Missing {TEST_FRAMEWORK} test coverage
- {DB_FRAMEWORK} usage anti-patterns
```

The project's AGENTS.md provides the bindings:

```markdown
## Stack Bindings
- LANGUAGE: Java 21
- FRAMEWORK: Spring Boot 3.5
- ARCHITECTURE: layered (Controller → Service → Repository)
- TEST_FRAMEWORK: JUnit 5 + Mockito
- DB_FRAMEWORK: Spring Data JDBC
```

### 6.4 Skill Dependency Documentation

When skills depend on each other, document the dependency chain:

```yaml
---
version: "1.0.0"
depends_on:
  - "code-review"       # references severity definitions
  - "java-conventions"  # references naming patterns
---
```

This prevents someone from deleting a skill that others depend on.

---

### 7. Anti-Patterns

### 7.1 Copy-Paste Prompts Between Projects

**Symptom:** You open a similar project, copy `skills` wholesale, and
tweak a few lines.

**Problem:** Within a month every project has a slightly different version.
Bug fixes in one project never reach the others. Nobody knows which
version is "correct."

**Fix:** Use a shared prompt library (Section 2.1) and reference it via
submodules, Stow, or chezmoi.

### 7.2 Chat-Only Prompts

**Symptom:** Your best prompts live in chat history, Slack messages,
or sticky notes.

**Problem:** No version history, no sharing, no reproducibility. If the
chat window closes or the message is lost, the prompt is gone.

**Fix:** Extract every reusable prompt into a SKILL.md or command file
and commit it to Git.

### 7.3 Monolithic System Prompt

**Symptom:** AGENTS.md is 200+ lines covering everything from code style
to database conventions to CI rules.

**Problem:** Lost-in-middle effect (Section 1) causes the model to
ignore rules buried in the center. Changes to one section risk breaking
unrelated behavior (Section 7, Section 2).

**Fix:** Split into composable skills (Section 9). Keep
AGENTS.md under ~40 lines for cross-cutting rules. Move domain-specific
instructions into focused skills.

### 7.4 No Metadata

**Symptom:** Prompt files contain raw instructions with no version,
no `tested_with`, no author, no changelog.

**Problem:** When a prompt breaks after a model upgrade, you have no way
to know which model version it was written for or what changed since.

**Fix:** Add YAML front-matter (Section 3) with at minimum `version`,
`tested_with`, and `changed` fields.

### 7.5 Global Rules That Should Be Project-Specific

**Symptom:** Your global `~/.agents/skills/code-review/SKILL.md`
references "Spring Data JDBC" -- but one of your projects uses JPA.

**Problem:** Global skills apply to every project. Project-specific
technology choices in global skills cause incorrect output in projects
that use a different stack.

**Fix:** Keep global skills technology-agnostic. Use template skills
(Section 9) or override patterns for stack-specific
rules. Alternatively, use direnv (Section 2.5) to switch skill sets
by project directory.

---

### Best Practices Summary

| Practice                                  | Why                                                        |
|-------------------------------------------|------------------------------------------------------------|
| Store all prompts in Git                  | Version history, collaboration, rollback                   |
| Use three layers (global/team/project)    | Right scope for each type of convention                    |
| Pin versions with submodules or tags      | Reproducible behavior; controlled updates                  |
| Add front-matter metadata                 | Know what model a skill was tested against                 |
| Use composable skills over monoliths      | Easier to test, share, and maintain independently          |
| Link with Stow or chezmoi, never copy     | Eliminates drift; single source of truth                   |
| Test shared prompts with promptfoo        | Catch regressions before they reach projects               |
| Start with Git + fzf + fd                 | Minimal tooling that covers 80% of needs                   |

---

### References

1. chezmoi. *Manage your dotfiles across multiple diverse machines*.
   [chezmoi.io](https://www.chezmoi.io/)

2. GNU Stow. *A symlink farm manager*.
   [gnu.org/software/stow](https://www.gnu.org/software/stow/)

3. direnv. *Unclutter your .profile*.
   [direnv.net](https://direnv.net/)

4. fzf. *A command-line fuzzy finder*.
   [github.com/junegunn/fzf](https://github.com/junegunn/fzf)

5. promptfoo. *Open-source LLM testing framework*.
   [promptfoo.dev](https://promptfoo.dev/)

6. Obsidian. *A knowledge base that works on local Markdown files*.
   [obsidian.md](https://obsidian.md/)

7. Logseq. *A privacy-first, open-source knowledge base*.
   [logseq.com](https://logseq.com/)

---

### 8. Prompt Versioning (Advanced)

### Semantic Versioning for Skills

Apply semantic versioning to each SKILL.md via the `version:` field in
YAML front-matter:

```yaml
---
version: "2.1.0"
---
```

| Version bump | When to use | Example |
|---|---|---|
| **Major** (`2.0.0` → `3.0.0`) | Breaking change to output format or behavior | Changed output from markdown table to JSON |
| **Minor** (`2.0.0` → `2.1.0`) | New capability added, backwards-compatible | Added support for `@WebFluxTest` alongside `@WebMvcTest` |
| **Patch** (`2.1.0` → `2.1.1`) | Wording refinement, typo fix, no behavior change | Clarified "use `@MockitoBean`" → "use `@MockitoBean` (not `@Mock`)" |

### Git Tags for Prompt Library Releases

Tag the prompt library repository when a set of skill changes is stable
and tested:

```bash
git tag -a prompts/v2.1.0 -m "Tested against Claude Sonnet 4 and GPT-4.1"
git push origin prompts/v2.1.0
```

Projects consuming the library via submodule can pin to this tag:

```bash
cd skills/shared
git checkout prompts/v2.1.0
cd ../..
git add skills/shared
git commit -m "chore: pin shared skills to prompts/v2.1.0"
```

### Changelog per Skill

Maintain a `CHANGELOG.md` at the prompt library root. Each entry records
what changed and why:

```markdown
## [2.1.0] - 2026-04-10

### code-review
- Added: support for reviewing sealed interface hierarchies
- Changed: severity of unused import from Warning to Info

### generate-tests
- Fixed: few-shot example used @Mock instead of @MockitoBean
```

### A/B Testing Prompts

When uncertain whether a change improves output quality, run both
versions in parallel:

1. Create a branch: `git checkout -b experiment/review-v3`
2. Make the change on the branch
3. Run the golden test set (Section 7) against both `main` and the branch
4. Compare outputs side-by-side using `promptfoo eval --compare`
5. Merge only if the new version is measurably better on the scorecard

---

### 9. Cross-Project Prompt Sharing Patterns

| Pattern | Pros | Cons | Best For |
|---------|------|------|----------|
| **Git submodule** | Version pinning, familiar Git workflow, PR reviews on updates | Complex update steps, merge conflicts on submodule pointer | Stable skills updated infrequently |
| **Symlinks / GNU Stow** | Simple setup, instant updates, zero copying | No version pinning, single-machine only | Single developer, local development |
| **Copy + customize** | Full control, no external dependencies | Drift across projects, no upstream fixes | Highly customized project-specific variants |
| **Package manager** (npm/pip private registry) | Version resolution, lockfile, CI-friendly | Overhead of packaging, non-native for markdown files | Large teams with many projects and CI pipelines |
| **Chezmoi** | Templating, multi-machine sync, encryption support | Learning curve, primarily designed for dotfiles | Developers working across multiple machines |

### Decision Guide

```text
Do you share prompts across machines?
├── Yes → Chezmoi (or Git submodule if team-wide)
└── No
    ├── Do you need version pinning?
    │   ├── Yes → Git submodule
    │   └── No → GNU Stow
    └── Do you need heavy customization per project?
        ├── Yes → Copy + customize (accept the drift)
        └── No → GNU Stow or Git submodule
```

---

### 10. Quick-Reference: Prompt Management Tasks

| Management Task | Tool | Command / Example |
|---|---|---|
| Create a prompt library | Git | `git init ~/prompt-library` |
| Add a skill to the library | Git | `cp SKILL.md ~/prompt-library/global/my-skill/` |
| Pin library version in a project | Git submodule | `git submodule add <url> skills/shared` |
| Link library globally (single machine) | GNU Stow | `stow -d ~/prompt-library -t ~/.agents/skills global` |
| Sync prompts across machines | Chezmoi | `chezmoi add ~/.agents/skills/code-review/SKILL.md` |
| Switch skill sets per directory | Direnv | `echo 'export AGENTS_SKILLS_PATH=...' > .envrc` |
| Search all prompts for a keyword | Ripgrep | `rg "@Transactional" ~/prompt-library` |
| Fuzzy-find and copy a skill | fzf + fd | `fp` (shell function from §5 above) |
| Run regression tests on prompts | promptfoo | `promptfoo eval && promptfoo view` |
| List skill versions | Shell + grep | `skill-versions` (shell function from §5 above) |
| Tag a stable release | Git | `git tag -a prompts/v2.1.0 -m "message"` |
| Update pinned submodule | Git | `cd skills/shared && git pull origin main` |
| Undo global linking | GNU Stow | `stow -D -d ~/prompt-library -t ~/.agents/skills global` |

---

## Next Section

Proceed to [Section 4: Prompt Techniques](04-prompt-techniques.md) for a
deep dive into prompting techniques, best practices, and production patterns
with practical Java/Spring Boot examples.

## Next Steps

- Apply these techniques to your existing `AGENTS.md` (Section 8)
- Add few-shot examples to your skills (Section 9)
- Rewrite one command using CO-STAR (Section 10)
- Create a `prompt-library` repo and link global skills with GNU Stow or chezmoi
- Once prompts fail or drift, see [Section 7: Prompt Optimization](07-prompt-optimization.md)
  for debugging, golden test sets, and promptfoo evaluations
- For team-wide sharing, see [Section 15: Team Collaboration](15-team-collaboration.md)
