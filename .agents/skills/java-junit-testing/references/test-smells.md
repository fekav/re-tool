# Test Smells — Before / After

Read this while reviewing or writing tests, to catch the patterns that make a test suite
expensive to maintain even though every test currently passes.

## One test, several unrelated behaviors

**Before:**
```java
@Test
void testOrder() {
    Order order = orderService.create(customer, items);
    assertThat(order.total()).isEqualTo(100.0);
    assertThat(order.status()).isEqualTo(OrderStatus.CREATED);

    orderService.cancel(order.id());
    assertThat(orderRepository.findById(order.id()).get().status())
        .isEqualTo(OrderStatus.CANCELLED);
}
```
This fails for two completely different reasons under one name, and a reader has to parse
the whole method to know which behavior actually broke.

**After:** split by behavior.
```java
@Test
void createsOrderWithCorrectTotal() { ... }

@Test
void cancellingOrderUpdatesStatus() { ... }
```

## Mocking plain value objects

**Before:**
```java
Address mockAddress = mock(Address.class);
when(mockAddress.city()).thenReturn("Berlin");
```

**After:** just construct it — there's no behavior worth faking.
```java
Address address = new Address("Main St 1", "Berlin", "10115");
```

## Asserting on private/internal state

**Before:** using reflection or a package-private getter added solely for the test to peek
at an internal field.

**After:** assert on the class's public contract — its return values, thrown exceptions, or
observable effects on a collaborator. If the only way to verify behavior is reaching into
private state, that's a sign the class's public API is missing something it should expose.

## Logic inside the test

**Before:**
```java
@Test
void appliesDiscountCorrectly() {
    for (Customer customer : testCustomers) {
        if (customer.isLoyaltyMember()) {
            assertThat(pricingService.price(customer, order).total()).isLessThan(100.0);
        }
    }
}
```
A loop or conditional inside a test means the test itself has untested logic, and a failure
doesn't say which customer caused it.

**After:** use `@ParameterizedTest` so each case runs and reports independently (see
`parameterized-tests.md`).

## Duplicated setup across many tests

**Before:** every test re-builds the same five-field `Customer` by hand with slightly
different inline values, making it hard to tell what's actually relevant to each test.

**After:** extract a test-data builder or "object mother" that defaults to a normal valid
case, and only overrides what each test cares about:
```java
Customer customer = CustomerTestData.builder().loyaltyMember(true).build();
```
This also makes tests resilient to a new required field being added to `Customer` later —
one place to update instead of dozens.

## Non-descriptive names

**Before:** `test1()`, `testSomething()`, `worksCorrectly()`.

**After:** name the behavior and condition, e.g. `withdraw_insufficientFunds_throwsException()`,
or a short method name plus `@DisplayName("withdraw() throws when funds are insufficient")`.

## Tests that pass for the wrong reason

A test that asserts only `assertThat(result).isNotNull()` after a complex calculation will
pass even if the calculation is completely wrong. Assert on the actual expected value, not
just "something came back."
