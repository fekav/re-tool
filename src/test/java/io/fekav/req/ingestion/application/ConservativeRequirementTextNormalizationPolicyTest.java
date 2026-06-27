package io.fekav.req.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConservativeRequirementTextNormalizationPolicyTest {

    private final ConservativeRequirementTextNormalizationPolicy policy =
        new ConservativeRequirementTextNormalizationPolicy();

    @Test
    void normalizesTextConservatively_whenTextHasLineEndingsAndSurroundingWhitespace() {
        String result = policy.normalize("  The system shall export reports.\r\nIt shall include CSV.\r  ");

        assertThat(result).isEqualTo("The system shall export reports.\nIt shall include CSV.");
    }

    @Test
    void returnsBlankText_whenTextIsBlank() {
        String result = policy.normalize("   ");

        assertThat(result).isEqualTo("   ");
    }
}
