---
name: java-junit-testing
description: Write, review, and refactor JUnit 5 (Jupiter) tests for Java code following current best practices — Arrange-Act-Assert structure, naming and @DisplayName conventions, AssertJ-style fluent assertions, Mockito mocking discipline, parameterized/data-driven tests, lifecycle management, and avoiding test smells like over-mocking or flaky tests. Use this skill whenever the user asks to write, add, fix, or review unit tests, integration tests, or test cases for Java classes, or mentions JUnit, Mockito, AssertJ, TDD, test coverage, or "testing this class/service/method" — even if they never say the word "JUnit" explicitly.
---

# Java JUnit Testing (JUnit 5 / Jupiter)

This skill governs how to write and review Java tests. The single most important habit it
teaches is: **inspect the project before writing a single line of test code.** Java test
suites vary a lot — JUnit version, assertion library, mocking style — and a test that doesn't
match the surrounding suite creates friction for the team that has to maintain it. Treat the
existing codebase as the source of truth; this document is the fallback when conventions
aren't already established.

## Step 0 — Read the project before writing anything

Before generating tests, spend one quick pass figuring out what's actually available:

1. **Find the build file** — `pom.xml` (Maven) or `build.gradle` / `build.gradle.kts` (Gradle).
2. **Check what's on the test classpath** — grep it for `junit`, `mockito`, `assertj`,
   `spring-boot-starter-test`, `testcontainers`, `hamcrest`. Don't assume AssertJ or Mockito
   are available just because they're popular; confirm the dependency exists, or that it's
   already used in an existing test.
3. **Open one existing `*Test.java` file** in the project, if one exists. Match its style:
   naming pattern, assertion library, whether it uses `@DisplayName`, how it sets up fixtures.
   Consistency with the existing suite beats any preference expressed below.
4. **Locate the test source root** — Maven/Gradle convention is `src/test/java/...` mirroring
   the package of the class under `src/main/java/...`. A test for
   `src/main/java/com/acme/billing/InvoiceService.java` belongs at
   `src/test/java/com/acme/billing/InvoiceServiceTest.java`.
5. **Identify what's already imported/used by JUnit 4** (`org.junit.Test`, `@Before`) vs
   **JUnit 5** (`org.junit.jupiter.api.Test`, `@BeforeEach`). If the project is still on
   JUnit 4, don't silently migrate it to JUnit 5 mid-task — flag it and ask, unless the user
   has already asked for a migration.

If nothing exists yet (new project, no test conventions), default to JUnit 5 + AssertJ +
Mockito — the de facto standard combination in the current Java ecosystem — and say so.

## Core structure: Arrange-Act-Assert

Every test follows the same three beats, ideally visually separated by a blank line:

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

One test verifies one behavior. If you find yourself writing "and" in the test name, or
asserting on two unrelated outcomes, split it into two tests — each failure should point at
exactly one thing that broke.

## Naming conventions

- **Class names:** `<ClassUnderTest>Test` for unit tests (e.g. `InvoiceServiceTest`).
  Reserve a distinct suffix such as `*IT` or `*IntegrationTest` for slower integration tests
  so build tools (Failsafe, Gradle test filters) and CI can run them as a separate, slower
  suite.
- **Method names:** describe behavior and condition, not implementation. Two patterns are
  both current and acceptable — pick whichever the project already uses:
  - `methodOrBehavior_condition_expectedResult()`, e.g.
    `withdraw_insufficientFunds_throwsException()`
  - Plain English via `@DisplayName`, with a short method name underneath:
    ```java
    @Test
    @DisplayName("withdraw() throws when funds are insufficient")
    void withdrawInsufficientFunds() { ... }
    ```
- Drop the legacy `test` prefix (`testWithdraw`) — it's a JUnit 3/4 holdover with no purpose
  under JUnit 5's annotation-based discovery.
- Use `@Nested` inner classes to group tests by scenario or starting state, which also
  produces readable nested output in `@DisplayName` reports:
  ```java
  @Nested
  class WhenAccountIsOverdrawn {
      @Test
      void rejectsFurtherWithdrawals() { ... }
  }
  ```

## Lifecycle

Use `@BeforeEach` / `@AfterEach` for per-test fixtures — this is the default and keeps tests
independent, since each test gets a fresh instance of the test class (JUnit 5's default
`Lifecycle.PER_METHOD`). Reach for `@BeforeAll` / `@AfterAll` only for genuinely expensive,
read-only setup (e.g. spinning up a shared container), and remember they must be `static`
unless the class is annotated `@TestInstance(Lifecycle.PER_CLASS)` — which itself is a
deliberate tradeoff (shared state across tests) and worth a one-line comment explaining why
it's needed.

## Assertions

If AssertJ is on the classpath (check the build file — don't assume), prefer it: it reads
close to plain English and produces much more informative failure messages than plain JUnit
assertions.

```java
assertThat(result.items()).hasSize(3).extracting(Item::sku).contains("SKU-1");
assertThatThrownBy(() -> account.withdraw(-10))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("negative");
```

If only JUnit 5's built-ins are available, use `assertEquals`, `assertThrows`, and
`assertAll` (the last one for grouping several independent checks so all of them run and
report, instead of stopping at the first failure).

See `references/assertions.md` for a fuller cheat sheet (collections, exceptions, objects,
floating point, soft assertions).

## Mocking with Mockito

A few hard rules that prevent the most common test-quality complaints in code review:

- **Only mock what you don't own** — collaborators that cross a boundary (repositories,
  HTTP clients, clocks, message publishers). Don't mock simple value objects, DTOs, or
  classes with no real behavior — just construct them.
- **Prefer constructor injection** with `@ExtendWith(MockitoExtension.class)` and
  `@Mock` / `@InjectMocks` over manual `Mockito.mock(...)` wiring or static-mocking tools —
  it's less code and fails fast if the constructor changes.
- **Assert on outcomes before verifying interactions.** If you can check the returned value
  or resulting state, do that first; reserve `verify(...)` for void methods and genuine side
  effects (e.g. "was the email actually sent").
- **Never silence `UnnecessaryStubbingException` with `lenient()`** as a first move — it's
  Mockito telling you a stub isn't used by this test. Delete the unused stub instead; adding
  `lenient()` papers over a real signal that the test has drifted from the code.

See `references/mocking.md` for mock vs. spy vs. fake guidance, `ArgumentCaptor`, and
BDD-style Mockito (`given(...).willReturn(...)`).

## Parameterized and data-driven tests

Don't copy-paste near-identical tests for different inputs — use `@ParameterizedTest`:

```java
@ParameterizedTest(name = "{0} is not a valid email")
@ValueSource(strings = {"", "no-at-sign", "@no-local-part.com"})
void rejectsInvalidEmail(String input) {
    assertThat(validator.isValid(input)).isFalse();
}
```

Use `@CsvSource` for small input/output pairs, `@MethodSource` once arguments stop fitting
on one line or need real objects, and `@EnumSource` / `@NullAndEmptySource` for exhaustive
edge-case coverage. Give each generated case a readable `name` so a failing run tells you
*which* input failed without opening the source. Full patterns and examples in
`references/parameterized-tests.md`.

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

A short list to flag while writing or reviewing tests — full before/after examples in
`references/test-smells.md`:

- A single test asserting several unrelated behaviors (split it).
- Mocking everything in sight, including plain value objects.
- Assertions on private/internal state instead of the public contract.
- Branching logic (`if`, loops, `try/catch` beyond what the API forces) inside a test —
  a test with its own logic is itself untested code.
- Large blocks of duplicated setup across many tests — extract to `@BeforeEach`, a helper
  method, or a test-data builder.
- Non-descriptive names (`test1`, `testSomething`, `worksCorrectly`).

## If the project uses Spring Boot

Don't reach for `@SpringBootTest` by default — it boots the full context and is the slowest
option. Prefer the narrower test slices (`@WebMvcTest`, `@DataJpaTest`) for layer-focused
tests, and reserve a full `@SpringBootTest` for genuine end-to-end checks. See
`references/spring-boot-testing.md` for slice-test selection, `MockMvc`, and
`@MockBean`/`@SpyBean` vs. constructor-based fakes.

## Before you call it done

- [ ] Test class mirrors the source package and follows the project's naming convention
- [ ] Method names / `@DisplayName` describe behavior, not implementation details
- [ ] Happy path, edge cases, and the exception path are all covered
- [ ] No unused stubs, no mocking of plain value objects
- [ ] Tests are independent of each other and of execution order
- [ ] Assertions are specific (checking actual values/messages, not just "is not null")
- [ ] Style matches the rest of the existing test suite in this repository
