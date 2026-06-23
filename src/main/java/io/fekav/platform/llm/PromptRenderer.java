package io.fekav.platform.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PromptRenderer {

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    public String render(Prompt prompt) {
        StringBuilder formattedPrompt = new StringBuilder();

        appendSection(formattedPrompt, "Task", prompt.taskDescription());
        appendSection(formattedPrompt, "Instructions", prompt.instructions());

        if (!prompt.outputSchema().isEmpty()) {
            appendSection(formattedPrompt, "Expected output schema", formatValue(prompt.outputSchema()));
        }

        if (!prompt.examples().isEmpty()) {
            formattedPrompt.append("Examples:\n");
            for (PromptExample example : prompt.examples()) {
                formattedPrompt.append("Input:\n").append(valueOrEmpty(example.input()).strip()).append("\n");
                formattedPrompt.append("Expected output:\n").append(formatValue(example.expectedOutput())).append("\n\n");
            }
        }

        appendSection(formattedPrompt, prompt.inputLabel(), prompt.input());

        return formattedPrompt.toString().stripTrailing();
    }

    private static void appendSection(StringBuilder prompt, String title, String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        prompt.append(labelOrDefault(title)).append(":\n")
                .append(content.strip())
                .append("\n\n");
    }

    private static String valueOrEmpty(String value) {
        return PromptValues.textOrEmpty(value);
    }

    private static String labelOrDefault(String value) {
        return PromptValues.labelOrDefault(value);
    }

    private static String formatValue(Object value) {
        if (value instanceof String text) {
            return text.strip();
        }

        try {
            return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("prompt value formatting failed", e);
        }
    }
}
