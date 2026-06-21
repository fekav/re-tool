# Quarkus Testing

Read this when the project under test is a Quarkus application. Quarkus's testing model
overlaps with Spring Boot's in spirit (test slices, mockable CDI beans) but the annotations,
mocking API, and tooling are different — don't port Spring idioms over by name.

## Picking the right test type

| Annotation | Loads | Use it for |
|---|---|---|
| Plain JUnit + Mockito (no Quarkus annotations) | Nothing — pure POJO test | Services and domain logic with no CDI injection to verify. **Prefer this whenever possible.** |
| `@QuarkusComponentTest` | Just the CDI beans you declare, not the full app | Unit-testing one or two beans together with their real or mocked collaborators, without paying for full application startup |
| `@QuarkusTest` | The full CDI container, in the same JVM as the test (fast — this is the point of Quarkus's testing model) | REST endpoint tests, integration tests against the running application |
| `@QuarkusIntegrationTest` | The packaged artifact (JAR or native image), started as a separate process | True end-to-end checks against what actually ships; the slowest option — use sparingly and tag accordingly |

## `@QuarkusTest` with REST Assured

REST Assured ships as the default way to exercise HTTP endpoints under `@QuarkusTest`:

```java
@QuarkusTest
class GreetingResourceTest {

    @Test
    void returnsGreeting_forGivenName() {
        given()
            .pathParam("name", "Ada")
        .when()
            .get("/hello/{name}")
        .then()
            .statusCode(200)
            .body(is("Hello, Ada"));
    }
}
```

## Mocking CDI beans: `@InjectMock`, not `@MockBean`

Quarkus's equivalent of Spring's `@MockBean` is `@InjectMock` (from
`io.quarkus.test.junit.mockito`). It replaces a CDI bean with a Mockito mock for the
duration of the test class:

```java
@QuarkusTest
class InvoiceResourceTest {

    @InjectMock
    PaymentGateway paymentGateway;

    @Test
    void returns402_whenPaymentFails() {
        when(paymentGateway.charge(any(), anyDouble())).thenReturn(PaymentResult.declined());

        given()
            .contentType(ContentType.JSON)
            .body(new ChargeRequest(1L, 50.0))
        .when()
            .post("/invoices/1/pay")
        .then()
            .statusCode(402);
    }
}
```

`@InjectSpy` is the spy equivalent, for wrapping a real bean while still being able to
`verify(...)` or override specific calls.

**CDI proxying constraint:** Quarkus mocks a bean by substituting its CDI proxy, so the bean
needs to be a normal injectable CDI bean (an interface implementation, or a class Arc can
proxy). A `final` class with no interface and no default constructor can be harder to mock
than under plain Mockito — if you hit this, consider extracting an interface for the
collaborator rather than fighting the proxy.

## Test profiles and config overrides

Use `@TestProfile` to run a test class against different configuration than the default
(e.g. a different feature flag or connection property), instead of hand-editing
`application.properties`:

```java
public class StrictValidationProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("app.validation.strict", "true");
    }
}

@QuarkusTest
@TestProfile(StrictValidationProfile.class)
class StrictValidationTest { ... }
```

For simpler cases, prefix properties with `%test.` in `application.properties` — Quarkus
applies these automatically whenever tests run, no profile class needed:
```properties
%test.quarkus.datasource.jdbc.url=jdbc:h2:mem:test
```

## Dev Services — don't hand-roll Testcontainers for the common cases

If a test needs a real database, Kafka broker, or similar, and no connection is configured,
Quarkus's **Dev Services** automatically starts a disposable container for it (backed by
Testcontainers under the hood) for the duration of the test run. This means most
integration tests against a database don't need any manual container setup at all — just
add the extension (e.g. `quarkus-jdbc-postgresql`) and leave the datasource URL unset in the
test profile. Reach for a hand-configured `@QuarkusTestResource` only when Dev Services
doesn't cover what you need (a non-standard external system, specific container startup
ordering, etc.).

```java
@QuarkusTestResource(WireMockServerResource.class)
@QuarkusTest
class ExternalApiClientTest { ... }
```

## Continuous testing

Quarkus dev mode (`./mvnw quarkus:dev` or `./gradlew quarkusDev`) re-runs the tests affected
by a change automatically in the background as you save files. This is a core part of the
intended workflow — when suggesting how to verify a change, it's worth mentioning this
rather than only "run the test suite," since it's faster feedback for iterative work.

## Testing reactive/Mutiny code

Don't block on a `Uni`/`Multi` with ad-hoc sleeps. Use the Mutiny test helpers:

```java
UniAssertSubscriber<String> subscriber = service.greetAsync("Ada")
    .subscribe().withSubscriber(UniAssertSubscriber.create());

subscriber.awaitItem().assertItem("Hello, Ada");
```

For a quick, synchronous-style assertion in a simple test, `.await().indefinitely()` on the
`Uni` is acceptable, but prefer the subscriber-based assertions above when you also need to
verify cancellation, failure, or timing behavior.

## Tag integration-style tests

Just as with plain JUnit, separate the fast `@QuarkusTest` suite from the slow
`@QuarkusIntegrationTest` suite with `@Tag("integration")` (or Quarkus's own
`@io.quarkus.test.junit.NativeImageTest`-related tagging conventions, if the project already
uses them) so CI can run them at different cadences.