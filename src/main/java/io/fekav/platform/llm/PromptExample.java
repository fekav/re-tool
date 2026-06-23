package io.fekav.platform.llm;

public record PromptExample(String input, Object expectedOutput) {

    public PromptExample {
        input = PromptValues.textOrEmpty(input);
        expectedOutput = PromptValues.copyValue(expectedOutput);
    }
}
