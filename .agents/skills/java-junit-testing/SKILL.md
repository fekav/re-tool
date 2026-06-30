---
name: java-junit-testing
description: Write, review, and refactor tests for Java code following current best practices. Use this skill whenever the user asks to write, add, fix, or review unit tests, integration tests, or test cases for Java classes, or "testing this class/service/method".
---

# Java Testing

This skill governs how to write and review Java tests. The single most important habit it
teaches is: **Writing simple, atomic cases testing only one behavior**.

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

## Lifecycle

- Use `@BeforeEach` / `@AfterEach` for per-test fixtures — this is the default and keeps tests independent, since each test gets a fresh instance of the test class.

## Assertions

Use AssertJ assertions when it's available in the project dependencies. Use junit assertions as fallback.

### Basic equality and truthiness

```java
assertThat(result).isEqualTo(42);
assertThat(flag).isTrue();
assertThat(name).isNotBlank();
```

### Objects

```java
assertThat(order)
    .extracting(Order::status, Order::total)
    .containsExactly(OrderStatus.PAID, 99.0);

// Field-by-field comparison, ignoring a generated id field
assertThat(actualOrder)
    .usingRecursiveComparison()
    .ignoringFields("id")
    .isEqualTo(expectedOrder);
```

`usingRecursiveComparison()` is preferable to overriding `equals()` on a domain class purely
for test convenience — keep production code free of test-only concerns.

### Collections

```java
assertThat(items).isEmpty();
assertThat(items).hasSize(3);
assertThat(items).containsExactly("a", "b", "c");      // order matters
assertThat(items).containsExactlyInAnyOrder("b", "a"); // order doesn't matter
assertThat(items).extracting(Item::sku).contains("SKU-1");
assertThat(items).allSatisfy(item -> assertThat(item.price()).isPositive());
```

### Exceptions

```java
// lets you chain checks on type and message
assertThatThrownBy(() -> account.withdraw(-10))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("negative");

```

Always assert on the exception **type and message** (or a relevant field on a custom
exception) — "an exception was thrown" alone doesn't prove it was thrown for the right
reason.

### Floating point

Never use plain `isEqualTo` on doubles/floats unless the value is an exact, deterministic
result of integer-like arithmetic.

```java
assertThat(total).isCloseTo(19.99, within(0.001));
```

### Grouping independent checks (soft assertions / assertAll)

When checking several independent properties of one result, group them so a run reports
*every* failure instead of stopping at the first:

```java
SoftAssertions.assertSoftly(softly -> {
    softly.assertThat(result.total()).isEqualTo(90.0);
    softly.assertThat(result.currency()).isEqualTo("EUR");
    softly.assertThat(result.discountApplied()).isTrue();
});
```

This is for several checks on **one** outcome — it's not a substitute for splitting a test
that covers genuinely unrelated behaviors.

### Optionals

```java
assertThat(repository.findById(1L)).isPresent().contains(expectedCustomer);
assertThat(repository.findById(999L)).isEmpty();
```


## Mocking Rules

A few hard rules that prevent the most common test-quality complaints in code review:

- **Only mock what you don't own** — collaborators that cross a boundary (repositories,
  HTTP clients, clocks, message publishers). Don't mock simple value objects, DTOs, or
  classes with no real behavior — just construct them.
- **Do not mock when writing end-to-end tests**
- **Constructor injection** with `@InjectMocks`, never manual `Mockito.mock(...)` wiring or static-mocking tools.
- **Never silence `UnnecessaryStubbingException` with `lenient()`** as a first move — it's
  Mockito telling you a stub isn't used by this test. Delete the unused stub instead; adding `lenient()` papers over a real signal that the test has drifted from the code.

## Parameterized and data-driven tests

When a test needs to run the same logic across several inputs, minimum 5 times, see `references/parameterized-tests.md`

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

## Red Flags

- Test class introduces new static classes.
- Mocking dependencies in a end-to-end test.
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

### Picking the right test type

| Annotation | Loads | Use it for |
|---|---|---|
| Plain JUnit + Mockito (no Quarkus annotations) | Nothing — pure POJO test | Services and domain logic with no CDI injection to verify. **Prefer this for testing domain logic.** |
| `@QuarkusComponentTest` | Just the CDI beans you declare, not the full app | Unit-testing one or two beans together with their real or mocked collaborators, without paying for full application startup |
| `@QuarkusTest` | The full CDI container, in the same JVM as the test (fast — this is the point of Quarkus's testing model) | REST endpoint tests, integration tests against the running application |
| `@QuarkusIntegrationTest` | The packaged artifact (JAR or native image), started as a separate process | True end-to-end checks against what actually ships; the slowest option — use sparingly and tag accordingly |

### `@QuarkusTest` with REST Assured

REST Assured ships as the default way to exercise HTTP endpoints under `@QuarkusTest`:

```java
package org.acme.user;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestHTTPEndpoint(UserResource.class)   // this tells RESTAssured to prefix all requests with resource's base URI
class UserResourceTest {

    @Test
    void list_returns_all_users() {
        List<UserDto> users =
                given()
                        .accept(ContentType.JSON)
                .when()
                        .get()          // relativ zu /users
                .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .extract()
                        .as(new TypeRef<>() {});

        assertThat(users)
                .containsExactly(
                        new UserDto(1L, "Ada"),
                        new UserDto(2L, "Linus")
                );
    }

    @Test
    void getById_returns_single_user() {
        UserDto user =
                given()
                        .accept(ContentType.JSON)
                .when()
                        .get("/{id}", 1) // relativ zu /users
                .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .extract()
                        .as(UserDto.class);

        assertThat(user)
                .isEqualTo(new UserDto(1L, "Ada"));
    }

    @Test
    void getById_returns_404_for_unknown_user() {
        given()
                .accept(ContentType.JSON)
        .when()
                .get("/{id}", 999)     // relativ zu /users
        .then()
                .statusCode(404);
    }

    record UserDto(Long id, String name) {
    }
}
```

### Mocking with `@InjectMock` from `io.quarkus.test.InjectMock`

**@Inject**: for testing CDI-Beans. 
**@InjectMock**: for mocking external dependencies

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PriceServiceTest {

    @Inject
    PriceService serviceUnderTest;

    @InjectMock
    TaxClient taxClient;

    @Test
    void calculatesGrossPrice() {
        // given
        when(taxClient.taxRateFor("DE")).thenReturn(0.19);

        // when
        Money result = serviceUnderTest.calculateGrossPrice(
            new Money("EUR", 100.00),
            "DE"
        );

        // then
        assertThat(result.currency()).isEqualTo("EUR");
        assertThat(result.amount()).isEqualTo(119.00);
        verify(taxClient).taxRateFor("DE");
    }
}
```

## Verification Checklist

- [ ] Test class mirrors the source package and follows the project's naming convention
- [ ] Method names / `@DisplayName` describe behavior, not implementation details
- [ ] Happy path, edge cases, and the exception path are all covered
- [ ] No unused stubs, no mocking of plain value objects
- [ ] Tests are independent of each other and of execution order
- [ ] Assertions are specific (checking actual values/messages, not just "is not null")
- [ ] Assertions used AssertJ when AssertJ-dependency exists in project
- [ ] Exception messages are tested
- [ ] Test code is structured in arrange-act-assert
