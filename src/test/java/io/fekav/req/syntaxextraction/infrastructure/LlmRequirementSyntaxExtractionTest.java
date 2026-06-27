package io.fekav.req.syntaxextraction.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.Prompt;
import io.fekav.platform.structuredoutput.InvalidStructuredOutputException;
import io.fekav.platform.structuredoutput.StructuredOutputValidationException;
import io.fekav.platform.structuredoutput.StructuredOutputValidator;
import io.fekav.req.shared.model.RawText;
import io.fekav.req.syntaxextraction.domain.RequirementSyntax;
import io.fekav.req.syntaxextraction.domain.RequirementSyntaxType;
import io.fekav.req.syntaxextraction.infrastructure.LlmRequirementSyntaxExtraction;

class LlmRequirementSyntaxExtractionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmClientPort llmClientPort = mock(LlmClientPort.class);
    private final LlmRequirementSyntaxExtraction extraction = new LlmRequirementSyntaxExtraction(
        llmClientPort,
        objectMapper,
        new StructuredOutputValidator()
    );

    @Test
    void returnsRequirementSyntax_whenLlmReturnsValidStructuredOutput() throws Exception {
        String requirementText = "The reporting dashboard shall export monthly usage metrics as CSV.";
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput(Map.of(
                "SUBJECT", "reporting dashboard",
                "ACTION", "shall export",
                "OBJECT", "monthly usage metrics",
                "CONSTRAINT", "as CSV",
                "CONDITION", ""
            ))));

        RequirementSyntax requirementSyntax =
            extraction.extractRequirementSyntax(new RawText(requirementText));

        assertThat(requirementSyntax.syntaxElements())
            .containsEntry(RequirementSyntaxType.SUBJECT, "reporting dashboard")
            .containsEntry(RequirementSyntaxType.ACTION, "shall export")
            .containsEntry(RequirementSyntaxType.OBJECT, "monthly usage metrics")
            .containsEntry(RequirementSyntaxType.CONSTRAINT, "as CSV")
            .doesNotContainKey(RequirementSyntaxType.CONDITION);

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        ArgumentCaptor<JsonNode> format = ArgumentCaptor.forClass(JsonNode.class);
        verify(llmClientPort).generate(prompt.capture(), format.capture());
        assertThat(prompt.getValue().promptText()).contains(requirementText);
        assertThat(format.getValue().toString())
            .contains("\"SUBJECT\"")
            .contains("\"ACTION\"")
            .contains("\"OBJECT\"");
    }

    @Test
    void returnsRequirementSyntax_whenConditionAndConstraintAreMissing() throws Exception {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput(Map.of(
                "SUBJECT", "reporting dashboard",
                "ACTION", "shall export",
                "OBJECT", "monthly usage metrics"
            ))));

        RequirementSyntax requirementSyntax = extraction.extractRequirementSyntax(
            new RawText("The reporting dashboard shall export monthly usage metrics.")
        );

        assertThat(requirementSyntax.syntaxElements())
            .containsEntry(RequirementSyntaxType.SUBJECT, "reporting dashboard")
            .containsEntry(RequirementSyntaxType.ACTION, "shall export")
            .containsEntry(RequirementSyntaxType.OBJECT, "monthly usage metrics")
            .doesNotContainKey(RequirementSyntaxType.CONSTRAINT)
            .doesNotContainKey(RequirementSyntaxType.CONDITION);
    }

    @Test
    void throwsInvalidStructuredOutput_whenLlmResponseIsNotJson() {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn("{");

        assertThatThrownBy(() ->
            extraction.extractRequirementSyntax(new RawText("The system shall export reports."))
        )
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("llm response is not valid JSON");
    }

    @Test
    void throwsInvalidStructuredOutput_whenLlmResponseHasNoModelOutput() {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn("{\"done\":true}");

        assertThatThrownBy(() ->
            extraction.extractRequirementSyntax(new RawText("The system shall export reports."))
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
            extraction.extractRequirementSyntax(new RawText("The dashboard shall export metrics."))
        )
            .isInstanceOf(StructuredOutputValidationException.class)
            .hasMessage("RequirementSyntaxOutput missing required fields: syntaxElements.ACTION");
    }

    private String llmResponse(String modelOutput) throws Exception {
        return objectMapper.writeValueAsString(Map.of("response", modelOutput));
    }

    private String modelOutput(Map<String, String> syntaxElements) throws Exception {
        return objectMapper.writeValueAsString(Map.of("syntaxElements", syntaxElements));
    }
}
