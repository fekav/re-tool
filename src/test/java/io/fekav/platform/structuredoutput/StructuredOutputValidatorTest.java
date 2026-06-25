package io.fekav.platform.structuredoutput;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class StructuredOutputValidatorTest {

    private final StructuredOutputValidator validator = new StructuredOutputValidator();

    @Test
    void acceptsOutput_whenRequiredTextIsPresent() {
        StructuredOutputContract<Map<String, String>> contract =
            StructuredOutputContract.<Map<String, String>>named("SampleOutput")
                .requiredText("subject", output -> output.get("subject"))
                .optionalText("condition", output -> output.get("condition"))
                .build();

        validator.validate(contract, Map.of("subject", "reporting dashboard"));
    }

    @Test
    void acceptsOutput_whenOptionalTextIsMissing() {
        StructuredOutputContract<Map<String, String>> contract =
            StructuredOutputContract.<Map<String, String>>named("SampleOutput")
                .requiredText("subject", output -> output.get("subject"))
                .optionalText("condition", output -> output.get("condition"))
                .build();

        validator.validate(contract, Map.of("subject", "reporting dashboard"));
    }

    @Test
    void throwsStructuredOutputValidation_whenRequiredTextIsBlank() {
        StructuredOutputContract<Map<String, String>> contract =
            StructuredOutputContract.<Map<String, String>>named("SampleOutput")
                .requiredText("subject", output -> output.get("subject"))
                .build();

        assertThatThrownBy(() -> validator.validate(contract, Map.of("subject", " ")))
            .isInstanceOf(StructuredOutputValidationException.class)
            .hasMessage("SampleOutput missing required fields: subject");
    }

    @Test
    void throwsStructuredOutputValidation_whenOutputIsNull() {
        StructuredOutputContract<Map<String, String>> contract =
            StructuredOutputContract.<Map<String, String>>named("SampleOutput")
                .requiredText("subject", output -> output.get("subject"))
                .build();

        assertThatThrownBy(() -> validator.validate(contract, null))
            .isInstanceOf(StructuredOutputValidationException.class)
            .hasMessage("SampleOutput is required");
    }
}
