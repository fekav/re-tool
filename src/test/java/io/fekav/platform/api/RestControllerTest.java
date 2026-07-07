package io.fekav.platform.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fekav.platform.cqrs.Command;
import io.fekav.platform.cqrs.CommandBus;
import io.fekav.platform.cqrs.CommandHandlerRegistry;
import io.fekav.platform.cqrs.Query;
import io.fekav.platform.cqrs.QueryBus;
import io.fekav.platform.cqrs.QueryHandlerRegistry;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
class RestControllerTest {

    @InjectMock
    CommandBus commandBus;

    @InjectMock
    CommandHandlerRegistry commandHandlerRegistry;

    @InjectMock
    QueryBus queryBus;

    @InjectMock
    QueryHandlerRegistry queryHandlerRegistry;

    @BeforeEach
    void resetMocks() {
        reset(commandBus, commandHandlerRegistry, queryBus, queryHandlerRegistry);
    }

    @Test
    void returnsCreatedResult_whenCommandDispatchSucceeds() {
        // Given
        doReturn(TestCommand.class)
            .when(commandHandlerRegistry)
            .commandType("TestCommand");
        when(commandBus.dispatch(new TestCommand("alpha")))
            .thenReturn(new TestResult("accepted"));

        // When
        TestResult result =
            given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(
                    """
                    {
                      "command": "TestCommand",
                      "payload": {
                        "value": "alpha"
                      }
                    }
                    """
                )
            .when()
                .post("/c")
            .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(TestResult.class);

        // Then
        assertThat(result).isEqualTo(new TestResult("accepted"));
    }

    @Test
    void dispatchesCommandCreatedFromObjectPayload_whenCommandIsKnown() {
        // Given
        doReturn(TestCommand.class)
            .when(commandHandlerRegistry)
            .commandType("TestCommand");

        // When
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(
                """
                {
                  "command": "TestCommand",
                  "payload": {
                    "value": "from object"
                  }
                }
                """
            )
        .when()
            .post("/c")
        .then()
            .statusCode(201);

        // Then
        verify(commandBus).dispatch(new TestCommand("from object"));
    }

    @Test
    void dispatchesCommandCreatedFromTextPayload_whenCommandIsKnown() {
        // Given
        doReturn(TestCommand.class)
            .when(commandHandlerRegistry)
            .commandType("TestCommand");

        // When
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(
                """
                {
                  "command": "TestCommand",
                  "payload": "{\\"value\\":\\"from text\\"}"
                }
                """
            )
        .when()
            .post("/c")
        .then()
            .statusCode(201);

        // Then
        verify(commandBus).dispatch(new TestCommand("from text"));
    }

    @Test
    void returnsBadRequest_whenRequestBodyIsNull() {
        // Given
        String requestBody = "null";

        // When
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/c")
        .then()
            .statusCode(400);

        // Then
        verifyNoInteractions(commandHandlerRegistry, commandBus);
    }

    @Test
    void returnsBadRequest_whenCommandIsMissing() {
        // Given
        String requestBody =
            """
            {
              "payload": {
                "value": "alpha"
              }
            }
            """;

        // When
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/c")
        .then()
            .statusCode(400);

        // Then
        verifyNoInteractions(commandHandlerRegistry, commandBus);
    }

    @Test
    void returnsBadRequest_whenPayloadIsMissing() {
        // Given
        String requestBody =
            """
            {
              "command": "TestCommand"
            }
            """;

        // When
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/c")
        .then()
            .statusCode(400);

        // Then
        verifyNoInteractions(commandHandlerRegistry, commandBus);
    }

    @Test
    void returnsBadRequest_whenCommandIsUnknown() {
        // Given
        when(commandHandlerRegistry.commandType("UnknownCommand"))
            .thenReturn(null);

        // When
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(
                """
                {
                  "command": "UnknownCommand",
                  "payload": {
                    "value": "alpha"
                  }
                }
                """
            )
        .when()
            .post("/c")
        .then()
            .statusCode(400);

        // Then
        verify(commandHandlerRegistry).commandType("UnknownCommand");
        verifyNoInteractions(commandBus);
    }

    @Test
    void returnsBadRequest_whenPayloadCannotCreateCommand() {
        // Given
        doReturn(IdCommand.class)
            .when(commandHandlerRegistry)
            .commandType("IdCommand");

        // When
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(
                """
                {
                  "command": "IdCommand",
                  "payload": {
                    "id": "not-a-uuid"
                  }
                }
                """
            )
        .when()
            .post("/c")
        .then()
            .statusCode(400);

        // Then
        verify(commandHandlerRegistry).commandType("IdCommand");
        verifyNoInteractions(commandBus);
    }

    @Test
    void returnsOkResult_whenQueryDispatchSucceeds() {
        // Given
        doReturn(TestQuery.class)
            .when(queryHandlerRegistry)
            .queryType("TestQuery");
        when(queryBus.execute(new TestQuery("alpha")))
            .thenReturn(new TestResult("found"));

        // When
        TestResult result =
            given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(
                    """
                    {
                      "query": "TestQuery",
                      "payload": {
                        "value": "alpha"
                      }
                    }
                    """
                )
            .when()
                .post("/q")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(TestResult.class);

        // Then
        assertThat(result).isEqualTo(new TestResult("found"));
    }

    @Test
    void dispatchesQueryCreatedFromTextPayload_whenQueryIsKnown() {
        // Given
        doReturn(TestQuery.class)
            .when(queryHandlerRegistry)
            .queryType("TestQuery");

        // When
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(
                """
                {
                  "query": "TestQuery",
                  "payload": "{\\"value\\":\\"from text\\"}"
                }
                """
            )
        .when()
            .post("/q")
        .then()
            .statusCode(200);

        // Then
        verify(queryBus).execute(new TestQuery("from text"));
    }

    @Test
    void returnsBadRequest_whenQueryIsMissing() {
        // Given
        String requestBody =
            """
            {
              "payload": {
                "value": "alpha"
              }
            }
            """;

        // When
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/q")
        .then()
            .statusCode(400);

        // Then
        verifyNoInteractions(queryHandlerRegistry, queryBus);
    }

    @Test
    void returnsBadRequest_whenQueryPayloadIsMissing() {
        // Given
        String requestBody =
            """
            {
              "query": "TestQuery"
            }
            """;

        // When
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/q")
        .then()
            .statusCode(400);

        // Then
        verifyNoInteractions(queryHandlerRegistry, queryBus);
    }

    @Test
    void returnsBadRequest_whenQueryIsUnknown() {
        // Given
        when(queryHandlerRegistry.queryType("UnknownQuery"))
            .thenThrow(new IllegalArgumentException(
                "No handler found for query: UnknownQuery"
            ));

        // When
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(
                """
                {
                  "query": "UnknownQuery",
                  "payload": {
                    "value": "alpha"
                  }
                }
                """
            )
        .when()
            .post("/q")
        .then()
            .statusCode(400);

        // Then
        verify(queryHandlerRegistry).queryType("UnknownQuery");
        verifyNoInteractions(queryBus);
    }

    @Test
    void returnsBadRequest_whenPayloadCannotCreateQuery() {
        // Given
        doReturn(IdQuery.class)
            .when(queryHandlerRegistry)
            .queryType("IdQuery");

        // When
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(
                """
                {
                  "query": "IdQuery",
                  "payload": {
                    "id": "not-a-uuid"
                  }
                }
                """
            )
        .when()
            .post("/q")
        .then()
            .statusCode(400);

        // Then
        verify(queryHandlerRegistry).queryType("IdQuery");
        verifyNoInteractions(queryBus);
    }

    public record TestCommand(String value) implements Command<TestResult> {}

    public record IdCommand(UUID id) implements Command<TestResult> {}

    public record TestQuery(String value) implements Query<TestResult> {}

    public record IdQuery(UUID id) implements Query<TestResult> {}

    public record TestResult(String status) {}
}
