package io.fekav.req.classification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.platform.structuredoutput.InvalidStructuredOutputException;
import io.fekav.platform.structuredoutput.StructuredOutputValidationException;
import io.fekav.platform.structuredoutput.StructuredOutputValidator;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.RequirementClassification;
import io.fekav.req.classification.domain.RequirementConceptType;
import io.fekav.req.classification.domain.RequirementProperty;

class RequirementClassificationOutputTest {

    private final StructuredOutputValidator validator = new StructuredOutputValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void schemaDefinesRequiredClassificationFields() throws Exception {
        JsonNode schema = readSchema();
        JsonNode classification = schema.path("properties").path("classification");

        assertThat(schema.path("required")).extracting(JsonNode::asText)
            .containsExactly("classification");
        assertThat(classification.path("required")).extracting(JsonNode::asText)
            .containsExactly("conceptType", "property", "confidenceScore", "rationale");
    }

    @Test
    void schemaDefinesSupportedConceptTypesAndProperties() throws Exception {
        JsonNode classificationProperties = readSchema()
            .path("properties")
            .path("classification")
            .path("properties");

        assertThat(classificationProperties.path("conceptType").path("enum")).extracting(JsonNode::asText)
            .containsExactly("GOAL", "NEED", "REQUIREMENT");
        assertThat(classificationProperties.path("property").path("enum")).extracting(JsonNode::asText)
            .containsExactly("FUNCTIONAL", "QUALITY");
    }

    @Test
    void schemaDefinesConfidenceScoreBoundsAndRationaleText() throws Exception {
        JsonNode classificationProperties = readSchema()
            .path("properties")
            .path("classification")
            .path("properties");

        JsonNode confidenceScore = classificationProperties.path("confidenceScore");
        assertThat(confidenceScore.path("type").asText()).isEqualTo("number");
        assertThat(confidenceScore.path("minimum").asDouble()).isEqualTo(0.0);
        assertThat(confidenceScore.path("maximum").asDouble()).isEqualTo(1.0);
        assertThat(classificationProperties.path("rationale").path("type").asText()).isEqualTo("string");
    }

    @Test
    void mapsRequirementClassificationOutputToDomainClassification() {
        RequirementClassificationOutput output = output(
            "REQUIREMENT",
            "QUALITY",
            0.93,
            "The sentence uses must and gives a measurable response-time constraint."
        );

        RequirementClassification classification = output.toRequirementClassification();

        assertThat(classification.conceptType()).isEqualTo(RequirementConceptType.REQUIREMENT);
        assertThat(classification.property()).isEqualTo(RequirementProperty.QUALITY);
        assertThat(classification.confidenceScore()).isEqualTo(new ConfidenceScore(0.93));
        assertThat(classification.rationale().text())
            .isEqualTo("The sentence uses must and gives a measurable response-time constraint.");
    }

    @Test
    void validatesMissingRequiredClassificationFields() {
        RequirementClassificationOutput output = new RequirementClassificationOutput(
            new RequirementClassificationFieldsOutput(null, null, null, null)
        );

        assertThatThrownBy(() -> validator.validate(RequirementClassificationOutput.contract(), output))
            .isInstanceOf(StructuredOutputValidationException.class)
            .hasMessage(
                "RequirementClassificationOutput missing required fields: " +
                    "classification.conceptType, classification.property, " +
                    "classification.confidenceScore, classification.rationale"
            );
    }

    @Test
    void rejectsMissingClassificationObject() {
        assertThatThrownBy(() ->
            validator.validate(RequirementClassificationOutput.contract(), new RequirementClassificationOutput(null))
        )
            .isInstanceOf(StructuredOutputValidationException.class)
            .hasMessage(
                "RequirementClassificationOutput missing required fields: " +
                    "classification.conceptType, classification.property, " +
                    "classification.confidenceScore, classification.rationale"
            );
    }

    @Test
    void rejectsUnsupportedConceptTypeAsInvalidStructuredOutput() {
        RequirementClassificationOutput output = output(
            "OUTCOME",
            "QUALITY",
            0.93,
            "The sentence names a desired outcome and a quality concern."
        );

        assertThatThrownBy(output::toRequirementClassification)
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("unsupported requirement concept type: OUTCOME");
    }

    @Test
    void rejectsUnsupportedPropertyAsInvalidStructuredOutput() {
        RequirementClassificationOutput output = output(
            "REQUIREMENT",
            "PERFORMANCE",
            0.93,
            "The sentence gives a measurable performance constraint."
        );

        assertThatThrownBy(output::toRequirementClassification)
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("unsupported requirement property: PERFORMANCE");
    }

    @Test
    void rejectsInvalidConfidenceScoreAsInvalidStructuredOutput() {
        RequirementClassificationOutput output = output(
            "REQUIREMENT",
            "QUALITY",
            1.01,
            "The sentence gives a measurable performance constraint."
        );

        assertThatThrownBy(output::toRequirementClassification)
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("classification confidence score is invalid");
    }

    @Test
    void rejectsBlankRationaleAsInvalidStructuredOutput() {
        RequirementClassificationOutput output = output(
            "REQUIREMENT",
            "QUALITY",
            0.93,
            " "
        );

        assertThatThrownBy(output::toRequirementClassification)
            .isInstanceOf(InvalidStructuredOutputException.class)
            .hasMessage("classification rationale is invalid");
    }

    private RequirementClassificationOutput output(
        String conceptType,
        String property,
        Double confidenceScore,
        String rationale
    ) {
        return new RequirementClassificationOutput(
            new RequirementClassificationFieldsOutput(conceptType, property, confidenceScore, rationale)
        );
    }

    private JsonNode readSchema() throws Exception {
        try (InputStream input = RequirementClassificationOutputTest.class.getResourceAsStream(
            "/contracts/ai/v1/requirement-classification.schema.json"
        )) {
            assertThat(input).isNotNull();
            return objectMapper.readTree(input);
        }
    }
}
