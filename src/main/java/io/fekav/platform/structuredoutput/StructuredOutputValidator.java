package io.fekav.platform.structuredoutput;

import java.util.List;
import java.util.Objects;

import org.jboss.logging.Logger;

import io.fekav.platform.observability.Observability;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StructuredOutputValidator {

    private static final Logger log = Logger.getLogger(StructuredOutputValidator.class);

    public <T> void validate(StructuredOutputContract<T> contract, T output) {
        long startNanos = System.nanoTime();
        Objects.requireNonNull(contract, "contract must not be null");

        try {
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

            log.info(
                Observability.event("structured_output.validation.end") + " " +
                    Observability.kv("contract", contract.name()) + " " +
                    Observability.kv("status", "ok") + " " +
                    Observability.kv("duration_ms", Observability.durationMs(startNanos))
            );
        } catch (RuntimeException | Error e) {
            log.error(
                Observability.event("structured_output.validation.end") + " " +
                    Observability.kv("contract", contract.name()) + " " +
                    Observability.kv("status", "error") + " " +
                    Observability.kv("duration_ms", Observability.durationMs(startNanos)) + " " +
                    Observability.kv("error_type", e.getClass().getSimpleName()) + " " +
                    Observability.kv("error_message", e.getMessage())
            );
            throw e;
        }
    }
}
