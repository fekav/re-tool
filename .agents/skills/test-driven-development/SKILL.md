---
name: test-driven-development
description: Drives development with tests. Use when you implementing new logic. Use not when you're about to modify existing functionality.
---

# Test-Driven Development

## Overview

Write a failing test before writing the code that makes it pass. For bug fixes, reproduce the bug with a test before attempting a fix. Tests are proof — "seems right" is not done. A codebase with good tests is an AI agent's superpower; a codebase without tests is a liability.

## When to Use

- Implementing any new logic or behavior
- Fixing any bug (the Prove-It Pattern)
- Modifying existing functionality
- Adding edge case handling
- Any change that could break existing behavior

**When NOT to use:** Pure configuration changes, documentation updates, or static content changes that have no behavioral impact.

**Required for Java tests:** When this TDD cycle involves writing, modifying, or reviewing Java tests, invoke and follow the `java-junit-testing` skill before changing those tests. This skill controls the TDD workflow; `java-junit-testing` controls Java/JUnit test design and implementation.

## The TDD Cycle

``` 
    RED                GREEN              REFACTOR
 Write a test    Write minimal code    Clean up the
 that fails  ──→  to make it pass  ──→  implementation  ──→  (repeat)
      │                  │                    │
      ▼                  ▼                    ▼
   Test FAILS        Test PASSES         Tests still PASS
```

### Step 1: RED — Write a Failing Test

Write the test first. It must fail. A test that passes immediately proves nothing.

```java
@Test
void shouldApplyDiscount_whenCustomerIsLoyaltyMember() {
    // Arrange
    Customer customer = CustomerTestData.loyaltyMember();
    Order order = new Order(customer, List.of(new LineItem("SKU-1", 100.0)));

    // Act
    PricedOrder result = pricingService.price(order);

    // Assert
    assertThat(result.total()).isEqualTo(90.0);
}
```

### Step 2: GREEN — Make It Pass

Write the minimum code to make the test pass. Don't over-engineer:

```java
public PricedOrder price(Order order) {
    double total = order.lineItems()
        .stream()
        .mapToDouble(LineItem::price)
        .sum();

    if (order.customer().isLoyaltyMember()) {
        total = total * 0.9;
    }

    return new PricedOrder(order.id(), total);
}
```

### Step 3: REFACTOR — Clean Up

With tests green, improve the code without changing behavior:

- Extract shared logic
- Improve naming
- Remove duplication
- Optimize if necessary

Run tests after every refactor step to confirm nothing broke.

## The Prove-It Pattern (Bug Fixes)

When a bug is reported, **do not start by trying to fix it.** Start by writing a test that reproduces it.

```
Bug report arrives
       │
       ▼
  Write a test that demonstrates the bug
       │
       ▼
  Test FAILS (confirming the bug exists)
       │
       ▼
  Implement the fix
       │
       ▼
  Test PASSES (proving the fix works)
       │
       ▼
  Run full test suite (no regressions)
```

**Example:**

```java
@Test
void setsCompletedAt_whenTaskIsCompleted() {
    // Arrange
    Task task = taskService.createTask(new CreateTaskCommand("Test"));

    // Act
    Task completed = taskService.completeTask(task.id());

    // Assert
    assertThat(completed.status()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(completed.completedAt()).isEqualTo(fixedInstant);
}
```

```java
public Task completeTask(TaskId id) {
    Task task = taskRepository.getById(id);
    Task completed = task.complete(clock.instant());

    taskRepository.save(completed);

    return completed;
}
```

## The Test Pyramid

Invest testing effort according to the pyramid — most tests should be small and fast, with progressively fewer tests at higher levels:

```
          ╱╲
         ╱  ╲         E2E Tests (~5%)
        ╱    ╲        Full user flows, real browser
       ╱──────╲
      ╱        ╲      Integration Tests (~15%)
     ╱          ╲     Component interactions, API boundaries
    ╱────────────╲
   ╱              ╲   Unit Tests (~80%)
  ╱                ╲  Pure logic, isolated, milliseconds each
 ╱──────────────────╲
```

**The Beyonce Rule:** If you liked it, you should have put a test on it. Infrastructure changes, refactoring, and migrations are not responsible for catching your bugs — your tests are. If a change breaks your code and you didn't have a test for it, that's on you.

### Test Sizes (Resource Model)

Beyond the pyramid levels, classify tests by what resources they consume:

| Size | Constraints | Speed | Example |
|------|------------|-------|---------|
| **Small** | Single process, no I/O, no network, no database | Milliseconds | Pure function tests, data transforms |
| **Medium** | Multi-process OK, localhost only, no external services | Seconds | API tests with test DB, component tests |
| **Large** | Multi-machine OK, external services allowed | Minutes | E2E tests, performance benchmarks, staging integration |

Small tests should make up the vast majority of your suite. They're fast, reliable, and easy to debug when they fail.

### Decision Guide

```
Is it pure logic with no side effects?
  → Unit test (small)

Does it cross a boundary (API, database, file system)?
  → Integration test (medium)

Is it a critical user flow that must work end-to-end?
  → E2E test (large) — limit these to critical paths
```
## Writing Java Tests

For Java test work, apply the TDD cycle in this skill and the Java/JUnit guidance from `java-junit-testing` together:

- Use this skill to decide the RED, GREEN, and REFACTOR sequence.
- Use `java-junit-testing` to design, name, structure, and implement the JUnit tests.
- Treat a Java test written without applying `java-junit-testing` as incomplete skill usage.
- Structure every Java test with the project's chosen Arrange-Act-Assert vocabulary. If the existing suite uses `// Given`, `// When`, `// Then`, use that consistently in each new or modified test.
- Do not mix language examples or framework idioms. When working in Java, use JUnit/AssertJ/Mockito/Quarkus patterns from `java-junit-testing`, not TypeScript, Jest, Spring, or ad hoc pseudocode.

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "I'll write tests after the code works" | You won't. And tests written after the fact test implementation, not behavior. |
| "This is too simple to test" | Simple code gets complicated. The test documents the expected behavior. |
| "Tests slow me down" | Tests slow you down now. They speed you up every time you change the code later. |
| "I tested it manually" | Manual testing doesn't persist. Tomorrow's change might break it with no way to know. |
| "The code is self-explanatory" | Tests ARE the specification. They document what the code should do, not what it does. |
| "It's just a prototype" | Prototypes become production code. Tests from day one prevent the "test debt" crisis. |
| "Let me run the tests again just to be extra sure" | After a clean test run, repeating the same command adds nothing unless the code has changed since. Run again after subsequent edits, not as reassurance. |

## Red Flags

- Writing code without any corresponding tests
- Tests that pass on the first run (they may not be testing what you think)
- "All tests pass" but no tests were actually run
- Bug fixes without reproduction tests
- Tests that test framework behavior instead of application behavior
- Test names that don't describe the expected behavior
- Skipping tests to make the suite pass
- Running the same test command twice in a row without any intervening code change

## Verification

Before considering the implementation complete, verify the work and the test discipline:

- [ ] The RED step was real: the new or changed test failed before the production change, or the failure was explained if compilation could not proceed because the production type did not exist yet.
- [ ] Every new or changed behavior has a focused test at the right level of the test pyramid.
- [ ] Bug fixes include a reproduction test that failed for the reported bug before the fix.
- [ ] Test names describe observable behavior, not implementation details.
- [ ] Tests follow the relevant test-writing skill completely. For Java, `java-junit-testing` has been invoked and the tests use JUnit/AssertJ/Mockito/Quarkus patterns as appropriate.
- [ ] Every new or modified Java test is explicitly structured as Arrange-Act-Assert or the repository's equivalent `// Given`, `// When`, `// Then` style.
- [ ] Tests avoid unused stubs, skipped/disabled cases, broad mocks of value objects, framework-behavior assertions, and multiple unrelated behaviors in one test.
- [ ] The narrow test command for the changed area passes.
- [ ] The broader project verification command passes, using the repository's actual build tool and documented command.
- [ ] Coverage has not decreased if the project tracks coverage.

**Note:** Run each test command after a change that could affect the result. After a clean run, don't repeat the same command unless the code has changed since — re-running on unchanged code adds no confidence.
