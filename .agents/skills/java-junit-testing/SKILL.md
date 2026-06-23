---
name: java-junit-testing
description: Write, review, and refactor tests for Java code following current best practices. Use this skill whenever the user asks to write, add, fix, or review unit tests, integration tests, or test cases for Java classes, or "testing this class/service/method".
---

# Java Testing

This skill governs how to write and review Java tests. The single most important habit it
teaches is: **Writing simple, atomic test cases**.

Secondary, **Black-box testing**. This skill is designed for fluctuating code. Do not rely on implemetation details of code under test.

## Step 0 — Read test dependencies

Before generating tests, spend one quick pass figuring out what's actually available:

1. **Find the build file** — `pom.xml` (Maven) or `build.gradle` / `build.gradle.kts` (Gradle).
2. **Check test dependencies** - grep it for `junit`, `mockito`, `assertj`

It is best practice that unit tests are isolated. Because of that, do not inspect code, except the code under test.

## Core structure: Arrange-Act-Assert

Some teams prefer the given-when-then vocabulary for readability — match whatever the existing suite already uses.

```java
@Test
void shouldApplyDiscount_whenCustomerIsLoyaltyMember() {
    // Given
    Customer customer = CustomerTestData.loyaltyMember();
    Order order = new Order(customer, List.of(new LineItem("SKU-1", 100.0)));

    // When
    PricedOrder result = pricingService.price(order);

    // Then
    assertThat(result.total()).isEqualTo(90.0);
}
```

One test verifies one behavior. If you find yourself writing "and" in the test name, or
asserting on two unrelated outcomes, split it into two tests — each failure should point at
exactly one thing that broke.

## Lifecycle

- Use `@BeforeEach` / `@AfterEach` for per-test fixtures — this is the default and keeps tests independent, since each test gets a fresh instance of the test class.

## Assertions

If AssertJ is available, always prefer. 

See `references/assertions.md` for a full explanation.

## Mocking with Mockito

A few hard rules that prevent the most common test-quality complaints in code review:

- **Only mock what you don't own** — collaborators that cross a boundary (repositories,
  HTTP clients, clocks, message publishers). Don't mock simple value objects, DTOs, or
  classes with no real behavior — just construct them.
- **Prefer constructor injection** with `@InjectMocks` over manual `Mockito.mock(...)` wiring or static-mocking tools —
  it's less code and fails fast if the constructor changes.
- **Never silence `UnnecessaryStubbingException` with `lenient()`** as a first move — it's
  Mockito telling you a stub isn't used by this test. Delete the unused stub instead; adding `lenient()` papers over a real signal that the test has drifted from the code.

## Parameterized and data-driven tests

When a test needs to run the same logic across several inputs, see `references/parameterized-tests.md`

## Cover the edges, not just the happy path

For each method under test, deliberately decide whether each of these applies, rather than
defaulting to only the success case:

- Null / empty / blank inputs
- Empty collections, single-element collections, and "many" collections
- Boundary values (0, -1, max, just-over-the-limit)
- The exception path — assert on type *and* message, not just "something was thrown"

## Independence and avoiding flaky tests

- No shared mutable `static` state between tests; each test builds its own fixtures.
- Don't rely on test execution order. Reach for `@TestMethodOrder` only for a genuinely
  sequential integration scenario, and say so in a comment — it's the exception, not habit.
- Don't depend on real wall-clock time, real network calls, real filesystem I/O, or
  `Thread.sleep` for timing in unit tests. Inject a `Clock` (or similar seam) instead of
  calling `Instant.now()` directly in production code, so tests can fix time deterministically.
- Tag tests so CI can run them at the right speed: `@Tag("unit")` for fast, isolated tests
  and `@Tag("integration")` for anything touching a real database, container, or network —
  then filter by tag in the build file rather than mixing speeds in one suite.

## Test smells to catch in review

A short list to flag while writing or reviewing tests:

- A single test asserting several unrelated behaviors (split it).
- Mocking everything in sight, including plain value objects.
- Assertions on private/internal state instead of the public contract.
- Branching logic (`if`, loops, `try/catch` beyond what the API forces) inside a test —
  a test with its own logic is itself untested code.
- Large blocks of duplicated setup across many tests — extract to `@BeforeEach`, a helper
  method, or a test-data builder.
- Non-descriptive names (`test1`, `testSomething`, `worksCorrectly`), prefer pattern **returnsY_whenDidX, pocketIsEmpty_whenSpentTooMuch ** 
- Exceptions are not tested, even system under test throws 

## If the project uses Quarkus

Read `references/quarkus-testing.md` for more information

## Verification Checklist

- [ ] Test class mirrors the source package and follows the project's naming convention
- [ ] Method names / `@DisplayName` describe behavior, not implementation details
- [ ] Happy path, edge cases, and the exception path are all covered
- [ ] No unused stubs, no mocking of plain value objects
- [ ] Tests are independent of each other and of execution order
- [ ] Assertions are specific (checking actual values/messages, not just "is not null")
- [ ] Exception messages are tested
- [ ] Test code is structured in arrange-act-assert
