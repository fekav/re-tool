package io.fekav.platform.api;

import static io.restassured.RestAssured.given;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.extraction.domain.Action;
import io.fekav.req.extraction.domain.Condition;
import io.fekav.req.extraction.domain.Constraint;
import io.fekav.req.extraction.domain.Subject;
import io.fekav.req.extraction.domain.TargetObject;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.RequirementElement;
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

    static JsonNode executeCommandAsJson(
        ObjectMapper objectMapper,
        String requestBody
    ) throws JsonProcessingException {
        return objectMapper.readTree(postCommandForBody(requestBody));
    }

    static String rawTextCommandRequest(
        ObjectMapper objectMapper,
        String command,
        String rawText
    ) throws JsonProcessingException {
        return commandRequest(
            objectMapper,
            command,
            Map.of("rawText", rawText)
        );
    }

    static String selectedTermCommandRequest(
        ObjectMapper objectMapper,
        String command,
        RequirementElement selectedTerm
    ) throws JsonProcessingException {
        return commandRequest(
            objectMapper,
            command,
            Map.of("requirementElement", selectedTermPayload(selectedTerm))
        );
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

    static Map<String, Object> selectedTermPayload(RequirementElement selectedTerm) {
        return Map.of(
            "type",
            selectedTerm.type(),
            "text",
            selectedTerm.text()
        );
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
