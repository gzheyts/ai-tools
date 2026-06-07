Generate a complete REST endpoint for the specified entity following
the layered architecture from AGENTS.md.

Create all these files:

1. **Controller** (@RestController):
   - @RequestMapping("/api/v1/{entity-plural}")
   - OpenAPI annotations (@Tag, @Operation, @ApiResponse)
   - @Valid on request bodies
   - Delegates to Service

2. **Service** (interface):
   - Methods for CRUD operations
   - Uses DTOs for parameters and return types

3. **ServiceImpl** (@Service):
   - Implements Service interface
   - @Transactional on write operations
   - Uses Repository and Mapper
   - Proper error handling (EntityNotFoundException, ConflictException)
   - Logging at debug (entry) and info (events)

4. **Repository** (@Repository):
   - Extends CrudRepository (Spring Data JDBC)
   - Named query methods

5. **Request/Response DTOs** (Java records):
   - Compact canonical constructors for validation
   - @NotNull, @NotBlank annotations

6. **Mapper** (MapStruct):
   - @Mapper(componentModel = "spring")
   - Entity <-> DTO methods

7. **Test** (JUnit 5):
   - Extends AbstractComponentTest
   - Tests for all CRUD operations
   - ObjectMother for test data
