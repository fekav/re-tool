package io.fekav.req.classification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.Prompt;
import io.fekav.platform.structuredoutput.InvalidStructuredOutputException;
import io.fekav.platform.structuredoutput.StructuredOutputValidationException;
import io.fekav.platform.structuredoutput.StructuredOutputValidator;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.RequirementClassification;
import io.fekav.req.classification.domain.RequirementConceptType;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.shared.model.RawText;

class LlmRequirementClassificationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmClientPort llmClientPort = mock(LlmClientPort.class);
    private final LlmRequirementClassificationService classificationService =
        new LlmRequirementClassificationService(
            llmClientPort,
            objectMapper,
            new StructuredOutputValidator()
        );

    @Test
    void returnsRequirementClassification_whenLlmReturnsValidStructuredOutput() throws Exception {
        String requirementText = "The checkout page must load within 2 seconds on a 4G connection.";
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput(
                "REQUIREMENT",
                "QUALITY",
                0.93,
                "The sentence uses must and gives a measurable response-time constraint."
            )));

        RequirementClassification classification =
            classificationService.classifyRequirement(new RawText(requirementText));

        assertThat(classification.conceptType()).isEqualTo(RequirementConceptType.REQUIREMENT);
        assertThat(classification.property()).isEqualTo(RequirementProperty.QUALITY);
        assertThat(classification.confidenceScore()).isEqualTo(new ConfidenceScore(0.93));
        assertThat(classification.rationale().text())
            .isEqualTo("The sentence uses must and gives a measurable response-time constraint.");

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        ArgumentCaptor<JsonNode> format = ArgumentCaptor.forClass(JsonNode.class);
        verify(llmClientPort).generate(prompt.capture(), format.capture());
        assertThat(prompt.getValue().promptText()).contains(requirementText);
        assertThat(format.getValue().toString())
            .contains("\"conceptType\"")
            .contains("\"confidenceScore\"")
            .contains("\"GOAL\"")
            .contains("\"NEED\"")
            .contains("\"REQUIREMENT\"");
    }

    @Test
    void passesDeepCopyOfRequirementClassificationSchemaToLlm() throws Exception {
        List<JsonNode> formats = new ArrayList<>();
        AtomicInteger callCount = new AtomicInteger();
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenAnswer(invocation -> {
                JsonNode format = invocation.getArgument(1);
                formats.add(format);
                if (callCount.incrementAndGet() == 1) {
                    ((ObjectNode) format).put("mutatedByProvider", true);
                }
                return llmResponse(modelOutput(
                    "GOAL",
                    "FUNCTIONAL",
                    0.91,
                    "The text states a desired outcome."
                ));
            });

        classificationService.classifyRequirement(new RawText("Reduce checkout abandonment."));
        classificationService.classifyRequirement(new RawText("Reduce failed onboarding."));

        assertThat(formats).hasSize(2);
        assertThat(formats.get(0)).isNotSameAs(formats.get(1));
        assertThat(formats.get(1).has("mutatedByProvider")).isFalse();
    }

    @Test
    void promptContainsClassificationDefinitionsAndTieBreakers() throws Exception {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput(
                "REQUIREMENT",
                "QUALITY",
                0.93,
                "The sentence uses must and gives a measurable response-time constraint."
            )));

        classificationService.classifyRequirement(new RawText("The checkout page must load quickly."));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(llmClientPort).generate(prompt.capture(), any(JsonNode.class));
        assertThat(prompt.getValue().promptText())
            .contains("Concept type records intent and commitment level.")
            .contains("GOAL: Desired outcome or business objective.")
            .contains("NEED: Stakeholder need or capability gap.")
            .contains("REQUIREMENT: Binding product or system obligation.")
            .contains("Property is a separate axis from concept type.")
            .contains("FUNCTIONAL: Behavior, capability, workflow, operation, or interaction.")
            .contains("QUALITY: Quality attribute or constraint")
            .contains("Allowed conceptType values are exactly: GOAL, NEED, REQUIREMENT.")
            .contains("Allowed property values are exactly: FUNCTIONAL, QUALITY.")
            .contains("Prefer GOAL")
            .contains("Prefer NEED")
            .contains("Prefer REQUIREMENT")
            .contains("Prefer QUALITY when the quality constraint is the distinguishing obligation")
            .contains("Keywords are evidence cues, not sufficient by themselves.")
            .contains("0.90 to 1.00")
            .contains("Below 0.60")
            .contains("rationale must justify both the concept type and the property");
    }

    @Test
    void promptContainsFixtureBackedClassificationCases() throws Exception {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput(
                "GOAL",
                "FUNCTIONAL",
                0.91,
                "The text states a desired outcome."
            )));

        classificationService.classifyRequirement(new RawText("Reduce checkout abandonment."));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(llmClientPort).generate(prompt.capture(), any(JsonNode.class));
        String promptText = prompt.getValue().promptText();

        assertThat(promptText).contains(fixtureFragments());
    }

    @Test
    void throwsInvalidStructuredOutput_whenLlmResponseIsNotJson() {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn("{");

        assertThatThrownBy(() ->
            classificationService.classifyRequirement(new RawText("The system shall export reports."))
        )
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("llm response is not valid JSON");
    }

    @Test
    void throwsInvalidStructuredOutput_whenLlmResponseHasNoModelOutput() {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn("{\"done\":true}");

        assertThatThrownBy(() ->
            classificationService.classifyRequirement(new RawText("The system shall export reports."))
        )
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("llm response does not contain model output");
    }

    @Test
    void throwsInvalidStructuredOutput_whenModelOutputIsNotJson() throws Exception {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse("{"));

        assertThatThrownBy(() ->
            classificationService.classifyRequirement(new RawText("The system shall export reports."))
        )
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("model output is not valid JSON");
    }

    @Test
    void throwsStructuredOutputValidation_whenRequiredFieldIsMissing() throws Exception {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(objectMapper.writeValueAsString(Map.of(
                "classification",
                Map.of(
                    "conceptType", "REQUIREMENT",
                    "confidenceScore", 0.93,
                    "rationale", "The text assigns an obligation."
                )
            ))));

        assertThatThrownBy(() ->
            classificationService.classifyRequirement(new RawText("The system shall export reports."))
        )
            .isInstanceOf(StructuredOutputValidationException.class)
            .hasMessage("RequirementClassificationOutput missing required fields: classification.property");
    }

    @Test
    void throwsInvalidStructuredOutput_whenConceptTypeIsUnsupported() throws Exception {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput(
                "OUTCOME",
                "QUALITY",
                0.93,
                "The text names an outcome and a quality concern."
            )));

        assertThatThrownBy(() ->
            classificationService.classifyRequirement(new RawText("Improve checkout response time."))
        )
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("unsupported requirement concept type: OUTCOME");
    }

    @Test
    void throwsInvalidStructuredOutput_whenPropertyIsUnsupported() throws Exception {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput(
                "REQUIREMENT",
                "PERFORMANCE",
                0.93,
                "The text names a measurable performance constraint."
            )));

        assertThatThrownBy(() ->
            classificationService.classifyRequirement(new RawText("The page must load within 2 seconds."))
        )
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("unsupported requirement property: PERFORMANCE");
    }

    @Test
    void throwsInvalidStructuredOutput_whenConfidenceScoreIsInvalid() throws Exception {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput(
                "REQUIREMENT",
                "QUALITY",
                1.01,
                "The text names a measurable performance constraint."
            )));

        assertThatThrownBy(() ->
            classificationService.classifyRequirement(new RawText("The page must load within 2 seconds."))
        )
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("classification confidence score is invalid");
    }

    @Test
    void throwsStructuredOutputValidation_whenRationaleIsBlank() throws Exception {
        when(llmClientPort.generate(any(Prompt.class), any(JsonNode.class)))
            .thenReturn(llmResponse(modelOutput("REQUIREMENT", "QUALITY", 0.93, " ")));

        assertThatThrownBy(() ->
            classificationService.classifyRequirement(new RawText("The page must load within 2 seconds."))
        )
            .isInstanceOf(StructuredOutputValidationException.class)
            .hasMessage("RequirementClassificationOutput missing required fields: classification.rationale");
    }

    private String[] fixtureFragments() throws Exception {
        JsonNode cases = fixtureCases();
        List<String> fragments = new ArrayList<>();

        cases.forEach(classificationCase -> {
            fragments.add(classificationCase.path("rawText").asText());
            fragments.add("\"conceptType\": \"" + classificationCase.path("conceptType").asText() + "\"");
            fragments.add("\"property\": \"" + classificationCase.path("property").asText() + "\"");
        });

        return fragments.toArray(String[]::new);
    }

    private JsonNode fixtureCases() throws Exception {
        try (InputStream input = LlmRequirementClassificationServiceTest.class.getResourceAsStream(
            "/fixtures/requirement-classification-cases.json"
        )) {
            assertThat(input).isNotNull();
            return objectMapper.readTree(input);
        }
    }

    private String llmResponse(String modelOutput) throws Exception {
        return objectMapper.writeValueAsString(Map.of("response", modelOutput));
    }

    private String modelOutput(
        String conceptType,
        String property,
        double confidenceScore,
        String rationale
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "classification",
            Map.of(
                "conceptType", conceptType,
                "property", property,
                "confidenceScore", confidenceScore,
                "rationale", rationale
            )
        ));
    }
}
