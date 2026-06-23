package io.fekav.platform.llm;

import java.util.List;
import java.util.Map;

public final class PromptTemplate {

    private final String inputLabel;
    private final String taskDescription;
    private final String instructions;
    private final List<PromptExample> examples;
    private final Map<String, Object> outputSchema;

    private PromptTemplate(Builder builder) {
        this.inputLabel = PromptValues.labelOrDefault(builder.inputLabel);
        this.taskDescription = PromptValues.textOrEmpty(builder.taskDescription);
        this.instructions = PromptValues.textOrEmpty(builder.instructions);
        this.examples = PromptValues.copyExamples(builder.examples);
        this.outputSchema = PromptValues.copyObjectMap(builder.outputSchema);
    }

    public String inputLabel() {
        return inputLabel;
    }

    public String taskDescription() {
        return taskDescription;
    }

    public String instructions() {
        return instructions;
    }

    public List<PromptExample> examples() {
        return examples;
    }

    public Map<String, Object> outputSchema() {
        return outputSchema;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String inputLabel = "Input";
        private String taskDescription = "";
        private String instructions = "";
        private List<PromptExample> examples = List.of();
        private Map<String, Object> outputSchema = Map.of();

        private Builder() {
        }

        public Builder inputLabel(String value) {
            inputLabel = value;
            return this;
        }

        public Builder taskDescription(String value) {
            taskDescription = value;
            return this;
        }

        public Builder instructions(String value) {
            instructions = value;
            return this;
        }

        public Builder examples(List<PromptExample> value) {
            examples = value == null ? List.of() : value;
            return this;
        }

        public Builder outputSchema(Map<String, Object> value) {
            outputSchema = value == null ? Map.of() : value;
            return this;
        }

        public PromptTemplate build() {
            return new PromptTemplate(this);
        }
    }

}
