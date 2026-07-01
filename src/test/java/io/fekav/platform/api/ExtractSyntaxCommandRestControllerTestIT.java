package io.fekav.platform.api;

import static io.fekav.platform.api.RestControllerCommandTestSupport.action;
import static io.fekav.platform.api.RestControllerCommandTestSupport.commandRequest;
import static io.fekav.platform.api.RestControllerCommandTestSupport.postCommandForBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.shared.model.RawText;
import io.fekav.req.syntaxextraction.application.SyntaxExtraction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(RestController.class)
@Tag("integration")
class ExtractSyntaxCommandRestControllerTestIT {

    private static final String REQUIREMENT_TEXT =
        "If a customer cancels an order before shipment, the commerce system must refund " +
            "the payment within 24 hours.";
    private static final String SUBJECT = "commerce system";
    private static final String ACTION = "must refund";
    private static final String TARGET_OBJECT = "payment";
    private static final String CONSTRAINT = "within 24 hours";
    private static final String CONDITION = "customer cancels an order before shipment";

    @InjectMock
    SyntaxExtraction syntaxExtraction;

    ObjectMapper objectMapper;
    String requestBody;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        reset(syntaxExtraction);
        when(syntaxExtraction.extractSyntax(new RawText(REQUIREMENT_TEXT)))
            .thenReturn(action(SUBJECT, ACTION, TARGET_OBJECT, CONSTRAINT, CONDITION));
        requestBody = extractSyntaxCommandRequest(REQUIREMENT_TEXT);
    }

    @Test
    void returnsRawText_whenExtractSyntaxCommandIsPosted() throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at("/rawText/text").asText()).isEqualTo(REQUIREMENT_TEXT);
    }

    @Test
    void returnsSubject_whenExtractSyntaxCommandIsPosted() throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at("/action/subject/text").asText()).isEqualTo(SUBJECT);
    }

    @Test
    void returnsAction_whenExtractSyntaxCommandIsPosted() throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at("/action/actionText").asText()).isEqualTo(ACTION);
    }

    @Test
    void returnsObject_whenExtractSyntaxCommandIsPosted() throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at("/action/targetObject/text").asText()).isEqualTo(TARGET_OBJECT);
    }

    @Test
    void returnsConstraint_whenExtractSyntaxCommandIsPosted() throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at("/action/constraints/0/text").asText()).isEqualTo(CONSTRAINT);
    }

    @Test
    void returnsCondition_whenExtractSyntaxCommandIsPosted() throws Exception {
        JsonNode result = executeCommandAsJson(requestBody);

        assertThat(result.at("/action/conditions/0/text").asText()).isEqualTo(CONDITION);
    }

    @Test
    void returnsEmptyCondition_whenExtractedSyntaxHasNoCondition()
        throws Exception {
        String requirementText =
            "The reporting dashboard shall export monthly usage metrics as a CSV file.";
        when(syntaxExtraction.extractSyntax(new RawText(requirementText)))
            .thenReturn(action(
                "reporting dashboard",
                "shall export",
                "monthly usage metrics",
                "as a CSV file",
                ""
            ));

        JsonNode result = executeCommandAsJson(
            extractSyntaxCommandRequest(requirementText)
        );

        assertThat(result.at("/action/conditions").size()).isZero();
    }

    private JsonNode executeCommandAsJson(String requestBody) throws Exception {
        return objectMapper.readTree(postCommandForBody(requestBody));
    }

    private String extractSyntaxCommandRequest(String requirementText)
        throws Exception {
        return commandRequest(
            objectMapper,
            "ExtractSyntaxCommand",
            Map.of("rawText", requirementText)
        );
    }
}
