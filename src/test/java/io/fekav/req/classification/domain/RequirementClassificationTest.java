package io.fekav.req.classification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RequirementClassificationTest {

    @Test
    void acceptsRequirementClassification_whenAllClassificationEvidenceIsValid() {
        Classification classification = new Classification(
            RequirementType.REQUIREMENT,
            RequirementProperty.QUALITY,
            new ConfidenceScore(0.93),
            new Rationale("The sentence uses must and gives a measurable response-time constraint.")
        );

        assertThat(classification.conceptType()).isEqualTo(RequirementType.REQUIREMENT);
        assertThat(classification.property()).isEqualTo(RequirementProperty.QUALITY);
        assertThat(classification.confidenceScore()).isEqualTo(new ConfidenceScore(0.93));
        assertThat(classification.rationale())
            .isEqualTo(new Rationale(
                "The sentence uses must and gives a measurable response-time constraint."
            ));
    }

    @Test
    void requiresRequirementConceptType() {
        assertThatThrownBy(() -> new Classification(
            null,
            RequirementProperty.QUALITY,
            new ConfidenceScore(0.93),
            new Rationale("The sentence gives a measurable constraint.")
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("conceptType must not be null");
    }

    @Test
    void requiresRequirementProperty() {
        assertThatThrownBy(() -> new Classification(
            RequirementType.REQUIREMENT,
            null,
            new ConfidenceScore(0.93),
            new Rationale("The sentence gives a measurable constraint.")
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("property must not be null");
    }

    @Test
    void requiresConfidenceScore() {
        assertThatThrownBy(() -> new Classification(
            RequirementType.REQUIREMENT,
            RequirementProperty.QUALITY,
            null,
            new Rationale("The sentence gives a measurable constraint.")
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("confidenceScore must not be null");
    }

    @Test
    void requiresClassificationRationale() {
        assertThatThrownBy(() -> new Classification(
            RequirementType.REQUIREMENT,
            RequirementProperty.QUALITY,
            new ConfidenceScore(0.93),
            null
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("rationale must not be null");
    }

    @Test
    void supportsExactlyV1RequirementConceptTypes() {
        assertThat(RequirementType.values())
            .containsExactly(
                RequirementType.GOAL,
                RequirementType.NEED,
                RequirementType.REQUIREMENT
            );
    }

    @Test
    void supportsExactlyV1RequirementProperties() {
        assertThat(RequirementProperty.values())
            .containsExactly(
                RequirementProperty.FUNCTIONAL,
                RequirementProperty.QUALITY
            );
    }

    @ParameterizedTest
    @ValueSource(doubles = { 0.0, 0.5, 1.0 })
    void acceptsConfidenceScore_whenScoreIsInsideInclusiveRange(double score) {
        assertThat(new ConfidenceScore(score).value()).isEqualTo(score);
    }

    @ParameterizedTest
    @ValueSource(doubles = { -0.01, 1.01, Double.NaN })
    void throwsInvalidConfidenceScore_whenScoreIsOutsideInclusiveRange(double score) {
        assertThatThrownBy(() -> new ConfidenceScore(score))
            .isInstanceOf(InvalidConfidenceScoreException.class)
            .hasMessage("confidence score must be between 0.0 and 1.0");
    }

    @Test
    void stripsClassificationRationale() {
        assertThat(new Rationale(" rationale ").text()).isEqualTo("rationale");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void throwsInvalidClassificationRationale_whenRationaleIsBlank(String rationale) {
        assertThatThrownBy(() -> new Rationale(rationale))
            .isInstanceOf(InvalidClassificationRationaleException.class)
            .hasMessage("classification rationale must not be blank");
    }

    @Test
    void throwsInvalidClassificationRationale_whenRationaleIsNull() {
        assertThatThrownBy(() -> new Rationale(null))
            .isInstanceOf(InvalidClassificationRationaleException.class)
            .hasMessage("classification rationale must not be blank");
    }
}
