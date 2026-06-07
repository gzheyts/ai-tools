---
name: generate-tests
description: >
  Generate JUnit 5 tests for Java classes following project testing conventions.
  Use when asked to generate tests, create test class, write unit tests, or
  add test coverage. Handles both service and controller tests.
---

## Instructions

### Step 1: Analyze Target Class
1. Read the class to be tested.
2. Identify all public methods.
3. Note constructor dependencies (for mocking).
4. Check if it's a Controller, Service, or Repository.

### Step 2: Generate Test Class

#### Test Class Structure
```java
class {ClassName}Test extends AbstractComponentTest {

    // Dependencies injected or mocked

    @Nested
    class MethodNameTests {

        @Test
        void methodName_happyPath_expectedResult() {
            // given
            var input = ObjectMother.{builder}().build();

            // when
            var result = service.method(input);

            // then
            assertThat(result).isNotNull();
            assertThat(result.field()).isEqualTo(expected);
        }

        @Test
        void methodName_nullInput_throwsException() {
            // given / when / then
            assertThatThrownBy(() -> service.method(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
```

### Step 3: Generate Test Cases

For each public method, generate tests for:
1. **Happy path**: Valid input, expected output
2. **Null input**: Null arguments, expect exception or default
3. **Empty collections**: Empty lists/sets, verify behavior
4. **Boundary values**: Min/max values, edge cases
5. **Not found**: ID that doesn't exist, expect EntityNotFoundException
6. **Conflict**: Duplicate creation, expect ConflictException

### Rules
- Extend AbstractComponentTest for integration tests
- Use ObjectMother for test data builders
- Use AssertJ for assertions (assertThat, assertThatThrownBy)
- Use @Nested classes to group tests by method
- Use @Transactional for database tests (auto-rollback)
- Method naming: methodName_stateUnderTest_ExpectedBehavior
- Never hardcode UUIDs -- use UUID.randomUUID()
- Never hardcode timestamps -- use Instant.now()
