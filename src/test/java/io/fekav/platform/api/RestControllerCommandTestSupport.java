package io.fekav.platform.api;

import static io.restassured.RestAssured.given;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.shared.model.ElementId;
import io.fekav.req.syntaxextraction.domain.Action;
import io.fekav.req.syntaxextraction.domain.Condition;
import io.fekav.req.syntaxextraction.domain.Constraint;
import io.fekav.req.syntaxextraction.domain.Subject;
import io.fekav.req.syntaxextraction.domain.TargetObject;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

final class RestControllerCommandTestSupport {

    private RestControllerCommandTestSupport() {
    }

    static String commandRequest(
        ObjectMapper objectMapper,
        String command,
        Object payload
    ) throws JsonProcessingException {
        return objectMapper.writeValueAsString(Map.of(
            "command",
            command,
            "payload",
            payload
        ));
    }

    static ValidatableResponse postCommand(String requestBody) {
        return given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/c")
        .then();
    }

    static String postCommandForBody(String requestBody) {
        return postCommand(requestBody)
            .statusCode(201)
            .contentType(ContentType.JSON)
            .extract()
            .asString();
    }

    static <T> T postCommandForResponse(
        String requestBody,
        Class<T> responseType
    ) {
        return postCommand(requestBody)
            .statusCode(201)
            .contentType(ContentType.JSON)
            .extract()
            .as(responseType);
    }

    static Set<String> responseSet(String value) {
        return value.isBlank()
            ? Set.of()
            : Set.of(value);
    }

    static Action action(
        String subject,
        String action,
        String targetObject,
        String constraint,
        String condition
    ) {
        Set<Condition> conditions = condition.isBlank()
            ? Set.of()
            : Set.of(new Condition(condition));
        Set<Constraint> constraints = constraint.isBlank()
            ? Set.of()
            : Set.of(new Constraint(constraint));

        return new Action(
            ElementId.create(),
            action,
            new Subject(ElementId.create(), subject),
            new TargetObject(ElementId.create(), targetObject),
            conditions,
            constraints
        );
    }
}
