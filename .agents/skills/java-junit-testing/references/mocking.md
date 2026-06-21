# Mocking with Mockito

Read this when writing or reviewing tests that need test doubles. Covers when to use which
kind of double, the standard Mockito setup, and the patterns that come up most often.

## Choose the right double

| Double | Use it when |
|---|---|
| **Real object / value object** | Simple data holders, DTOs, domain values with no real side effects — just construct them. This is the default; reach for a mock only when this doesn't work. |
| **Fake** | A lightweight working implementation (e.g. an in-memory `Map`-backed repository) when you need realistic behavior across many tests. Often worth writing once and reusing. |
| **Stub (via Mockito `when(...).thenReturn(...)`)** | You need a collaborator to return a canned value so the test can proceed. |
| **Mock + `verify(...)`** | You need to confirm a side-effecting call happened (an email was sent, an event was published) and there's no observable return value to assert on instead. |
| **Spy** | You need most of the real object's behavior but want to override or verify one specific method. Use sparingly — it's easy to end up testing real + fake behavior tangled together. |

Only mock collaborators that cross a real boundary: repositories, HTTP/RPC clients, message
publishers, clocks, random sources, file/network I/O. Don't mock simple value objects or
anything with no meaningful behavior to fake.

## Standard setup

```java
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void chargesCustomer_whenInvoiceIsDue() {
        // Arrange
        Customer customer = CustomerTestData.standard();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // Act
        invoiceService.chargeInvoice(1L, 50.0);

        // Assert
        verify(paymentGateway).charge(customer, 50.0);
    }
}
```

`@InjectMocks` wires the `@Mock` fields into the constructor (or setters/fields, in that
priority order) of the class under test. If the constructor changes, this fails fast at
compile or run time rather than silently leaving a collaborator null.

## BDD-style Mockito

Some teams prefer the `given/when/then` vocabulary for readability — match whatever the
existing suite already uses:

```java
given(customerRepository.findById(1L)).willReturn(Optional.of(customer));

invoiceService.chargeInvoice(1L, 50.0);

then(paymentGateway).should().charge(customer, 50.0);
```

This is functionally identical to `when(...).thenReturn(...)` / `verify(...)` — it's a
naming convention (`org.mockito.BDDMockito`), not a different mocking model.

## ArgumentCaptor

Use this when you need to inspect *what* was passed to a mocked collaborator, not just that
it was called:

```java
ArgumentCaptor<Charge> chargeCaptor = ArgumentCaptor.forClass(Charge.class);
verify(paymentGateway).submit(chargeCaptor.capture());

assertThat(chargeCaptor.getValue().amount()).isEqualTo(50.0);
assertThat(chargeCaptor.getValue().currency()).isEqualTo("EUR");
```

## Things that signal a test needs rework, not a workaround

- **`UnnecessaryStubbingException`** — a stub you set up was never used by that test.
  Delete the stub; don't reach for `lenient()` as a first response. `lenient()` is for the
  rare case where a shared `@BeforeEach` setup legitimately stubs something only some tests
  in the class use — not a general fix for sloppy stubbing.
- **Long chains of mocks returning mocks** (`when(a.b()).thenReturn(c)`,
  `when(c.d()).thenReturn(e)`...) — usually means the class under test reaches too deep into
  a collaborator's internals. Consider whether the collaborator's interface should expose
  the result directly instead.
- **Mocking a concrete class with `final` methods** works in modern Mockito (inline mock
  maker is default since Mockito 5), but if you find yourself needing to mock a class that
  has no interface and lots of real logic, that's often a sign the dependency should be
  extracted behind a narrower interface instead.
- **Verifying every single interaction** (`verifyNoMoreInteractions`, exhaustive
  `verify()` calls on incidental methods) tends to make tests brittle against harmless
  refactors. Verify the interactions that matter to the behavior being tested, not all of
  them.
