package io.fekav.platform.llm;

import java.util.List;
import java.util.Map;

public record Prompt(
        String input,
        String inputLabel,
        String taskDescription,
        String instructions,
        List<Example> examples,
        Map<String, Object> outputSchema) {

    public record Example(String input, Object expectedOutput) {
    }
}
