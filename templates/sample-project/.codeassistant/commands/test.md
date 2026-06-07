---
description: Generate JUnit 5 tests following project conventions
argument-hint: <class-name>
---

Generate tests for the specified class.

Conventions: AbstractComponentTest base, ObjectMother data builders,
methodName_stateUnderTest_ExpectedBehavior naming, @Nested grouping,
AssertJ assertions, BDD structure.

Cover: happy path, null input, not found, empty collections, boundaries.
