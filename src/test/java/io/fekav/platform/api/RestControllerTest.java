package io.fekav.platform.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.fekav.platform.adapter.DefaultCommandBus;
import io.fekav.platform.cqrs.Command;
import io.fekav.platform.cqrs.CommandHandlerRegistry;
import ö;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
class RestControllerTest {

    @InjectMock
    DefaultCommandBus commandBus;

    CommandHandlerRegistry commandHandlerRegistry;

    @BeforeEach
    void installMocks() {
        commandBus = mock(DefaultCommandBus.class);
        commandHandlerRegistry = mock(CommandHandlerRegistry.class);

        QuarkusMock.installMockForType(commandBus, DefaultCommandBus.class);
        QuarkusMock.installMockForType(
            commandHandlerRegistry,
            CommandHandlerRegistry.class
        );
    }

    @Test
    void shouldDispatchCommand_whenPayloadIsJsonObject() {
        // Arrange
        TestResult expectedResult = new TestResult("created");
        doReturn(TestCommand.class).when(commandHandlerRegistry).commandType(
            "TestCommand"
        );
        when(commandBus.dispatch(any(TestCommand.class))).thenReturn(
            expectedResult
        );

        // Act
        TestResult response = given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "command": "TestCommand",
                  "payload": {
                    "name": "alpha"
                  }
                }
                """
            )
            .when()
            .post("/app/c")
            .then()
            .statusCode(Response.Status.CREATED.getStatusCode())
            .extract()
            .as(TestResult.class);

        // Assert
        assertThat(response).isEqualTo(expectedResult);
        ArgumentCaptor<TestCommand> commandCaptor = ArgumentCaptor.forClass(
            TestCommand.class
        );
        verify(commandBus).dispatch(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(new TestCommand("alpha"));
    }

    @Test
    void shouldDispatchCommand_whenPayloadIsTextualJson() {
        // Arrange
        TestResult expectedResult = new TestResult("created");
        doReturn(TestCommand.class).when(commandHandlerRegistry).commandType(
            "TestCommand"
        );
        when(commandBus.dispatch(any(TestCommand.class))).thenReturn(
            expectedResult
        );

        // Act
        TestResult response = given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "command": "TestCommand",
                  "payload": "{\\"name\\":\\"beta\\"}"
                }
                """
            )
            .when()
            .post("/app/c")
            .then()
            .statusCode(Response.Status.CREATED.getStatusCode())
            .extract()
            .as(TestResult.class);

        // Assert
        assertThat(response).isEqualTo(expectedResult);
        ArgumentCaptor<TestCommand> commandCaptor = ArgumentCaptor.forClass(
            TestCommand.class
        );
        verify(commandBus).dispatch(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(new TestCommand("beta"));
    }

    @Test
    void shouldRejectRequest_whenPayloadIsMissing() {
        // Arrange
        String requestBody = """
            {
              "command": "TestCommand"
            }
            """;

        // Act
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/app/c")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Assert
        verifyNoInteractions(commandHandlerRegistry, commandBus);
    }

    @Test
    void shouldRejectRequest_whenCommandIsUnknown() {
        // Arrange
        when(commandHandlerRegistry.commandType("UnknownCommand")).thenReturn(
            null
        );

        // Act
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "command": "UnknownCommand",
                  "payload": {
                    "name": "alpha"
                  }
                }
                """
            )
            .when()
            .post("/app/c")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Assert
        verifyNoInteractions(commandBus);
    }

    @Test
    void shouldRejectRequest_whenTextualPayloadIsMalformed() {
        // Arrange
        doReturn(TestCommand.class).when(commandHandlerRegistry).commandType(
            "TestCommand"
        );

        // Act
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "command": "TestCommand",
                  "payload": "{not-json"
                }
                """
            )
            .when()
            .post("/app/c")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Assert
        verifyNoInteractions(commandBus);
    }

    public record TestCommand(String name) implements Command<TestResult> {}

    public record TestResult(String status) {}
}
