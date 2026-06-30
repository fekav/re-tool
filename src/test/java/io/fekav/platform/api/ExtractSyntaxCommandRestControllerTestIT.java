package io.fekav.platform.api;

import static io.fekav.platform.api.RestControllerCommandTestSupport.action;
import static io.fekav.platform.api.RestControllerCommandTestSupport.commandRequest;
import static io.fekav.platform.api.RestControllerCommandTestSupport.postCommandForResponse;
import static io.fekav.platform.api.RestControllerCommandTestSupport.responseSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.req.shared.model.RawText;
import io.fekav.req.syntaxextraction.application.ExtractSyntaxResponse;
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
    void returnsSubject_whenExtractSyntaxCommandIsPosted() {
        ExtractSyntaxResponse result = executeCommand(requestBody);

        assertThat(result.requirementElements().SUBJECT()).isEqualTo(SUBJECT);
    }

    @Test
    void returnsAction_whenExtractSyntaxCommandIsPosted() {
        ExtractSyntaxResponse result = executeCommand(requestBody);

        assertThat(result.requirementElements().ACTION()).isEqualTo(ACTION);
    }

    @Test
    void returnsObject_whenExtractSyntaxCommandIsPosted() {
        ExtractSyntaxResponse result = executeCommand(requestBody);

        assertThat(result.requirementElements().OBJECT()).isEqualTo(TARGET_OBJECT);
    }

    @Test
    void returnsConstraint_whenExtractSyntaxCommandIsPosted() {
        ExtractSyntaxResponse result = executeCommand(requestBody);

        assertThat(result.requirementElements().CONSTRAINT()).isEqualTo(responseSet(CONSTRAINT));
    }

    @Test
    void returnsCondition_whenExtractSyntaxCommandIsPosted() {
        ExtractSyntaxResponse result = executeCommand(requestBody);

        assertThat(result.requirementElements().CONDITION()).isEqualTo(responseSet(CONDITION));
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

        ExtractSyntaxResponse result = executeCommand(
            extractSyntaxCommandRequest(requirementText)
        );

        assertThat(result.requirementElements().CONDITION()).isEmpty();
    }

    private ExtractSyntaxResponse executeCommand(String requestBody) {
        return postCommandForResponse(requestBody, ExtractSyntaxResponse.class);
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
