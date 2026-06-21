# Spring Boot Testing Slices

Read this when the project under test is a Spring Boot application. The core idea: pick the
narrowest slice that exercises the layer you actually care about — full application context
startup is slow and should be the exception, not the default.

## Picking the right slice

| Annotation | Loads | Use it for |
|---|---|---|
| Plain JUnit + Mockito (no Spring annotations) | Nothing — pure POJO test | Services, domain logic, anything with no Spring-managed wiring to verify. **Prefer this whenever possible.** |
| `@WebMvcTest(MyController.class)` | Just the web layer for one controller | Controller request/response mapping, validation, status codes |
| `@DataJpaTest` | JPA repositories + an embedded/test database | Repository query methods, custom `@Query` correctness |
| `@JsonTest` | Jackson serialization only | Custom serializers/deserializers, DTO JSON shape |
| `@SpringBootTest` | The full application context | True end-to-end checks; use sparingly, it's the slowest option |

## `@WebMvcTest` example

```java
@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    @Test
    void returns200WithInvoice_whenFound() throws Exception {
        when(invoiceService.findById(1L)).thenReturn(Optional.of(InvoiceTestData.standard()));

        mockMvc.perform(get("/invoices/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(100.0));
    }
}
```

Note: `@MockBean` is the long-standing Spring Boot annotation for replacing a bean with a
Mockito mock inside the test context. Newer Spring Boot versions (3.4+) introduce
`@MockitoBean` as the direct replacement — check which one the project's Spring Boot
version and existing tests actually use rather than assuming.

## `@DataJpaTest` example

```java
@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void findsCustomerByEmail() {
        customerRepository.save(new Customer("a@example.com", "Ada"));

        assertThat(customerRepository.findByEmail("a@example.com"))
            .isPresent()
            .get()
            .extracting(Customer::name)
            .isEqualTo("Ada");
    }
}
```

By default this rolls back the transaction after each test and uses an embedded database
unless one is explicitly excluded — confirm the project isn't relying on a real database
here before assuming that.

## When you genuinely need `@SpringBootTest`

Reserve it for tests that need to verify the wiring of the whole application — e.g. an
end-to-end flow through several real layers. Tag these distinctly
(`@Tag("integration")`) so CI can run them separately from the fast unit/slice suite:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
class InvoiceFlowIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    // ...
}
```

## Testcontainers for real-database integration tests

When a test genuinely needs to run against the real database engine (not an embedded
substitute), prefer Testcontainers over a shared, hand-maintained test database — it gives
every run a fresh, disposable instance and removes "works on my machine" drift. Only reach
for this for the integration-tagged suite, not for everyday unit tests.
