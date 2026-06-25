package io.fekav.platform.structuredoutput;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StructuredOutputValidator {

    public <T> void validate(StructuredOutputContract<T> contract, T output) {
        Objects.requireNonNull(contract, "contract must not be null");

        if (output == null) {
            throw new StructuredOutputValidationException(contract.name() + " is required");
        }

        List<String> missingRequiredFields = contract.textFields()
            .stream()
            .filter(textField -> textField.isMissingFrom(output))
            .map(StructuredOutputContract.TextField::path)
            .toList();

        if (!missingRequiredFields.isEmpty()) {
            throw new StructuredOutputValidationException(
                contract.name() + " missing required fields: " + String.join(", ", missingRequiredFields)
            );
        }
    }
}
