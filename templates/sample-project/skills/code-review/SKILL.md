---
name: code-review
description: >
  Review Java/Spring Boot code for architecture violations, SOLID principles,
  security issues, and performance problems. Use when asked to review code,
  audit a class, check for issues, or do a code review.
---

## Instructions

Review the provided Java code against the following checklist.

### Architecture
- [ ] Controllers handle HTTP only (no business logic)
- [ ] Services contain business logic with @Transactional where needed
- [ ] Repositories handle data access only
- [ ] DTOs are Java records; entities never exposed via REST
- [ ] Controllers do not inject Repositories directly
- [ ] MapStruct used for entity/DTO conversion

### Java 21 Idioms
- [ ] DTOs are records (not Lombok @Data)
- [ ] Pattern matching switch used instead of if-else chains
- [ ] Text blocks used for multi-line strings
- [ ] No raw types (generics used properly)

### Code Quality
- [ ] No wildcard imports
- [ ] Annotation order: Lombok -> Spring -> Validation -> Docs
- [ ] Methods under 30 lines
- [ ] Meaningful variable names
- [ ] Proper null safety (@NonNull/@Nullable, Optional)

### Security
- [ ] Input validated with @Valid on @RequestBody
- [ ] No SQL concatenation (parameterized queries only)
- [ ] Sensitive data not logged
- [ ] Proper exception handling (no stack traces in responses)

### Testing
- [ ] Test exists for the class
- [ ] Naming: methodName_stateUnderTest_ExpectedBehavior
- [ ] Edge cases covered (null, empty, boundary)
- [ ] ObjectMother used for test data

## Output Format

Group findings by severity:

### Critical (must fix before merge)
- File:Line -- Description -- Fix

### Warning (should fix)
- File:Line -- Description -- Fix

### Suggestion (nice to have)
- File:Line -- Description -- Fix

For each finding, show the problematic code and a corrected version.
