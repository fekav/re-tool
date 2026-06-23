package io.fekav.platform.llm;

import java.util.List;
import java.util.Map;

public record Prompt(
        String input,
        String inputLabel,
        String taskDescription,
        String instructions,
        List<PromptExample> examples,
        Map<String, Object> outputSchema) {

    public Prompt {
        input = PromptValues.textOrEmpty(input);
        inputLabel = PromptValues.labelOrDefault(inputLabel);
        taskDescription = PromptValues.textOrEmpty(taskDescription);
        instructions = PromptValues.textOrEmpty(instructions);
        examples = PromptValues.copyExamples(examples);
        outputSchema = PromptValues.copyObjectMap(outputSchema);
    }
}
