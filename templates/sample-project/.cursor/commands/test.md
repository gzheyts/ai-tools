Generate JUnit 5 tests for the specified class.

Follow these conventions:
- Test class extends AbstractComponentTest
- Method naming: methodName_stateUnderTest_ExpectedBehavior
- Use ObjectMother pattern for test data builders
- Use @Nested classes to group tests by method
- BDD structure with comments: // given, // when, // then
- Use AssertJ for assertions

Cover these scenarios for each public method:
1. Happy path (valid input, expected output)
2. Null input (expect IllegalArgumentException or appropriate handling)
3. Not found (expect EntityNotFoundException for lookups)
4. Edge cases (empty strings, boundary values, empty collections)

Use @Transactional on integration tests for auto-rollback.
Never hardcode UUIDs or timestamps.
