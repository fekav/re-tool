# Assertions Cheat Sheet (JUnit 5 + AssertJ)

Read this when you need concrete assertion syntax beyond what SKILL.md covers. Prefer
AssertJ when it's on the classpath; otherwise use the JUnit 5 equivalents shown alongside.

## Basic equality and truthiness

```java
// AssertJ
assertThat(result).isEqualTo(42);
assertThat(flag).isTrue();
assertThat(name).isNotBlank();

// JUnit 5 built-in
assertEquals(42, result);
assertTrue(flag);
```

## Objects

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

## Collections

```java
assertThat(items).isEmpty();
assertThat(items).hasSize(3);
assertThat(items).containsExactly("a", "b", "c");      // order matters
assertThat(items).containsExactlyInAnyOrder("b", "a"); // order doesn't matter
assertThat(items).extracting(Item::sku).contains("SKU-1");
assertThat(items).allSatisfy(item -> assertThat(item.price()).isPositive());
```

## Exceptions

```java
// AssertJ — preferred, lets you chain checks on type and message
assertThatThrownBy(() -> account.withdraw(-10))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("negative");

// JUnit 5 built-in — useful when you need the exception instance afterward
IllegalArgumentException ex = assertThrows(
    IllegalArgumentException.class,
    () -> account.withdraw(-10)
);
assertThat(ex.getMessage()).contains("negative");

// No exception should be thrown
assertThatCode(() -> account.withdraw(10)).doesNotThrowAnyException();
```

Always assert on the exception **type and message** (or a relevant field on a custom
exception) — "an exception was thrown" alone doesn't prove it was thrown for the right
reason.

## Floating point

Never use plain `isEqualTo` on doubles/floats unless the value is an exact, deterministic
result of integer-like arithmetic.

```java
assertThat(total).isCloseTo(19.99, within(0.001));
```

## Grouping independent checks (soft assertions / assertAll)

When checking several independent properties of one result, group them so a run reports
*every* failure instead of stopping at the first:

```java
// AssertJ
SoftAssertions.assertSoftly(softly -> {
    softly.assertThat(result.total()).isEqualTo(90.0);
    softly.assertThat(result.currency()).isEqualTo("EUR");
    softly.assertThat(result.discountApplied()).isTrue();
});

// JUnit 5 built-in
assertAll(
    () -> assertEquals(90.0, result.total()),
    () -> assertEquals("EUR", result.currency()),
    () -> assertTrue(result.discountApplied())
);
```

This is for several checks on **one** outcome — it's not a substitute for splitting a test
that covers genuinely unrelated behaviors.

## Optionals

```java
assertThat(repository.findById(1L)).isPresent().contains(expectedCustomer);
assertThat(repository.findById(999L)).isEmpty();
```
