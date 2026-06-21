# Parameterized and Data-Driven Tests

Read this when a test needs to run the same logic across several inputs. The goal is always
the same: one failing case should tell you exactly which input failed, without needing to
open the test source.

## `@ValueSource` — a single primitive/String argument

```java
@ParameterizedTest(name = "[{index}] \"{0}\" is not a valid email")
@ValueSource(strings = {"", "no-at-sign", "@no-local-part.com"})
void rejectsInvalidEmail(String input) {
    assertThat(validator.isValid(input)).isFalse();
}
```

## `@CsvSource` — small input/expected-output pairs

```java
@ParameterizedTest(name = "{0} + {1} = {2}")
@CsvSource({
    "1, 1, 2",
    "2, 3, 5",
    "-1, 1, 0"
})
void addsTwoNumbers(int a, int b, int expectedSum) {
    assertThat(calculator.add(a, b)).isEqualTo(expectedSum);
}
```

Use `@CsvFileSource(resources = "/test-data/discounts.csv")` once the table grows past a
handful of rows — keeps the test method readable and the data easy to extend.

## `@MethodSource` — real objects, or arguments too complex for a CSV line

```java
@ParameterizedTest
@MethodSource("invalidOrders")
void rejectsInvalidOrder(Order order, String expectedReason) {
    assertThatThrownBy(() -> orderValidator.validate(order))
        .hasMessageContaining(expectedReason);
}

static Stream<Arguments> invalidOrders() {
    return Stream.of(
        Arguments.of(OrderTestData.withNoLineItems(), "must contain at least one item"),
        Arguments.of(OrderTestData.withNegativeQuantity(), "quantity must be positive")
    );
}
```

`@MethodSource` methods must be `static` unless the class is `@TestInstance(PER_CLASS)`.

## `@EnumSource` — exhaustive coverage of an enum

```java
@ParameterizedTest
@EnumSource(OrderStatus.class)
void everyStatusHasADisplayLabel(OrderStatus status) {
    assertThat(status.displayLabel()).isNotBlank();
}
```

Narrow it when only some values apply: `@EnumSource(value = OrderStatus.class, names =
{"PAID", "REFUNDED"})`.

## `@NullSource` / `@EmptySource` / `@NullAndEmptySource`

Use these instead of hand-rolling null/empty cases — they make the edge case explicit in
the annotation rather than buried in a `@ValueSource` list:

```java
@ParameterizedTest
@NullAndEmptySource
void rejectsBlankCustomerName(String name) {
    assertThat(validator.isValid(name)).isFalse();
}
```

## Naming generated cases

Always set a `name` pattern on `@ParameterizedTest` once there's more than one argument, so
a failure in a CI log already tells you which case broke:

```java
@ParameterizedTest(name = "{index}: {0} -> expects {1}")
```

`{index}` is the 1-based invocation index, `{0}`, `{1}`, ... refer to the method's
parameters in order.
