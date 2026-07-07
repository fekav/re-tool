package io.fekav.req.syntaxextraction.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.Prompt;
import io.fekav.platform.structuredoutput.InvalidStructuredOutputException;
import io.fekav.platform.structuredoutput.StructuredOutputValidationException;
import io.fekav.platform.structuredoutput.StructuredOutputValidator;
import io.fekav.req.extraction.domain.Action;
import io.fekav.req.extraction.domain.Condition;
import io.fekav.req.extraction.domain.Constraint;
import io.fekav.req.extraction.infrastructure.LlmSyntaxExtraction;
import io.fekav.req.shared.model.RawText;

class LlmSyntaxExtractionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmClientPort llmClientPort = mock(LlmClientPort.class);
    private final LlmSyntaxExtraction extraction = new LlmSyntaxExtraction(
        llmClientPort,
        objectMapper,
        new StructuredOutputValidator()
    );

    @Test
    void returnsAction_whenLlmReturnsValidStructuredOutput() throws Exception {
        String requirementText = "The reporting dashboard shall export monthly usage metrics as CSV.";
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput(Map.of(
                "SUBJECT", "reporting dashboard",
                "ACTION", "shall export",
                "OBJECT", "monthly usage metrics",
                "CONSTRAINT", "as CSV",
                "CONDITION", ""
            ))));

        Action action =
            extraction.extractSyntax(new RawText(requirementText));

        assertThat(action.subject().text()).isEqualTo("reporting dashboard");
        assertThat(action.actionText()).isEqualTo("shall export");
        assertThat(action.targetObject().text()).isEqualTo("monthly usage metrics");
        assertThat(action.constraints()).containsExactly(new Constraint("as CSV"));
        assertThat(action.conditions()).isEmpty();

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        ArgumentCaptor<JsonNode> format = ArgumentCaptor.forClass(JsonNode.class);
        verify(llmClientPort).generate(prompt.capture(), format.capture());
        assertThat(prompt.getValue().promptText()).contains(requirementText);
        assertThat(format.getValue().toString())
            .contains("\"SUBJECT\"")
            .contains("\"ACTION\"")
            .contains("\"OBJECT\"")
            .contains("\"CONDITION\"")
            .contains("\"CONSTRAINT\"");
    }

    @Test
    void returnsActionWithEmptyQualifiers_whenConditionAndConstraintAreMissing() throws Exception {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput(Map.of(
                "SUBJECT", "reporting dashboard",
                "ACTION", "shall export",
                "OBJECT", "monthly usage metrics"
            ))));

        Action action = extraction.extractSyntax(
            new RawText("The reporting dashboard shall export monthly usage metrics.")
        );

        assertThat(action.subject().text()).isEqualTo("reporting dashboard");
        assertThat(action.actionText()).isEqualTo("shall export");
        assertThat(action.targetObject().text()).isEqualTo("monthly usage metrics");
        assertThat(action.constraints()).isEmpty();
        assertThat(action.conditions()).isEmpty();
    }

    @Test
    void mapsNonBlankConditionAndConstraintToOneElementSets() throws Exception {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput(Map.of(
                "SUBJECT", "monitoring service",
                "ACTION", "must notify",
                "OBJECT", "operator",
                "CONSTRAINT", "immediately",
                "CONDITION", "sensor temperature exceeds 80 degrees Celsius"
            ))));

        Action action = extraction.extractSyntax(
            new RawText("When sensor temperature exceeds 80 degrees Celsius, the monitoring service must notify the operator immediately.")
        );

        assertThat(action.conditions())
            .isEqualTo(Set.of(new Condition("sensor temperature exceeds 80 degrees Celsius")));
        assertThat(action.constraints()).isEqualTo(Set.of(new Constraint("immediately")));
    }

    @Test
    void throwsInvalidStructuredOutput_whenLlmResponseIsNotJson() {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn("{");

        assertThatThrownBy(() ->
            extraction.extractSyntax(new RawText("The system shall export reports."))
        )
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("llm response is not valid JSON");
    }

    @Test
    void throwsInvalidStructuredOutput_whenLlmResponseHasNoModelOutput() {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn("{\"done\":true}");

        assertThatThrownBy(() ->
            extraction.extractSyntax(new RawText("The system shall export reports."))
        )
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("llm response does not contain model output");
    }

    @Test
    void throwsStructuredOutputValidation_whenRequiredSyntaxElementIsMissing() throws Exception {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput(Map.of(
                "SUBJECT", "reporting dashboard",
                "OBJECT", "monthly usage metrics"
            ))));

        assertThatThrownBy(() ->
            extraction.extractSyntax(new RawText("The dashboard shall export metrics."))
        )
            .isInstanceOf(StructuredOutputValidationException.class)
            .hasMessage("SyntaxExtractionOutput missing required fields: requirementElements.ACTION");
    }

    private String llmResponse(String modelOutput) throws Exception {
        return objectMapper.writeValueAsString(Map.of("response", modelOutput));
    }

    private String modelOutput(Map<String, String> requirementElements) throws Exception {
        return objectMapper.writeValueAsString(Map.of("requirementElements", requirementElements));
    }
}
