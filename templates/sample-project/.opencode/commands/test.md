---
description: Generate JUnit 5 tests for a Java class
---

Generate JUnit 5 tests for $1.

Conventions:
- Extend AbstractComponentTest
- Naming: methodName_stateUnderTest_ExpectedBehavior
- ObjectMother for test data
- @Nested classes per method
- BDD: given / when / then
- AssertJ assertions
- @Transactional for DB tests

Cover: happy path, null input, not found, edge cases.
