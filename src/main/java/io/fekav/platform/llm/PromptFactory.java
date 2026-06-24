package io.fekav.platform.llm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Factory for building Prompt instances.
 *
 * Template variables use the format: {{variableName}}
 */
@ApplicationScoped
public final class PromptFactory {

    private PromptFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder fromTemplate(String template) {
        return builder().template(template);
    }

    public static final class Builder {

        private String systemPrompt;
        private String promptText;
        private String template;

        private final Map<String, String> variables = new LinkedHashMap<>();
        private final List<FewShotExample> fewShotExamples = new ArrayList<>();

        private Builder() {
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = requireNonBlank(systemPrompt, "systemPrompt");
            return this;
        }

        public Builder promptText(String promptText) {
            this.promptText = requireNonBlank(promptText, "promptText");
            return this;
        }

        public Builder template(String template) {
            this.template = requireNonBlank(template, "template");
            return this;
        }

        public Builder variable(String name, Object value) {
            String variableName = requireNonBlank(name, "name");
            Objects.requireNonNull(value, "value must not be null");

            variables.put(variableName, String.valueOf(value));
            return this;
        }

        public Builder variables(Map<String, ?> variables) {
            Objects.requireNonNull(variables, "variables must not be null");

            variables.forEach(this::variable);
            return this;
        }

        public Builder addFewShotExample(String input, String output) {
            fewShotExamples.add(new FewShotExample(
                    requireNonBlank(input, "input"),
                    requireNonBlank(output, "output")
            ));
            return this;
        }

        public Prompt build() {
            String resolvedPromptText = template != null
                    ? renderTemplate(template, variables)
                    : requireNonBlank(promptText, "promptText");

            return new Prompt(
                    prependFewShotExamples(resolvedPromptText),
                    requireNonBlank(systemPrompt, "systemPrompt")
            );
        }

        private String renderTemplate(String template, Map<String, String> variables) {
            String rendered = template;

            for (Map.Entry<String, String> entry : variables.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }

            if (rendered.matches("(?s).*\\{\\{[^{}]+}}.*")) {
                throw new IllegalStateException("Template contains unresolved variables: " + rendered);
            }

            return rendered;
        }

        private String prependFewShotExamples(String basePromptText) {
            if (fewShotExamples.isEmpty()) {
                return basePromptText;
            }

            StringBuilder result = new StringBuilder();

            result.append("Examples:\n\n");

            for (int i = 0; i < fewShotExamples.size(); i++) {
                FewShotExample example = fewShotExamples.get(i);

                result.append("Example ")
                        .append(i + 1)
                        .append(":\n")
                        .append("Input:\n")
                        .append(example.input())
                        .append("\n\n")
                        .append("Output:\n")
                        .append(example.output())
                        .append("\n\n");
            }

            result.append("Prompt:\n")
                    .append(basePromptText);

            return result.toString();
        }

        private static String requireNonBlank(String value, String fieldName) {
            Objects.requireNonNull(value, fieldName + " must not be null");

            if (value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }

            return value;
        }
    }

    private record FewShotExample(String input, String output) {
    }
}