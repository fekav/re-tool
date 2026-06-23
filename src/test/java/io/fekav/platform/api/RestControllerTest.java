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

    @BeforeEach
    void resetMocks() {
        reset(commandBus, commandHandlerRegistry);
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

    public record TestCommand(String value) implements Command<TestResult> {}

    public record IdCommand(UUID id) implements Command<TestResult> {}

    public record TestResult(String status) {}
}
