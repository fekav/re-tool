package io.fekav.platform.llm;

import java.util.Objects;

/**
 * Immutable prompt data model.
 *
 * @param promptText   the final user prompt text sent to the LLM
 * @param systemPrompt the system-level instruction text
 */
public record Prompt(String promptText, String systemPrompt) {

    public Prompt {
        promptText = requireNonBlank(promptText, "promptText");
        systemPrompt = requireNonBlank(systemPrompt, "systemPrompt");
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value;
    }
}