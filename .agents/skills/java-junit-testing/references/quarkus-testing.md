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

## Mocking with `@InjectMock` from `io.quarkus.test.InjectMock`

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