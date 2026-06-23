package io.fekav.platform.llm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PromptFactory {

    private final PromptRenderer renderer;

    @Inject
    public PromptFactory(PromptRenderer renderer) {
        this.renderer = renderer;
    }

    public Prompt create(PromptTemplate template, String input) {
        return new Prompt(
                input,
                template.inputLabel(),
                template.taskDescription(),
                template.instructions(),
                template.examples(),
                template.outputSchema());
    }

    public LlmRequest createRequest(PromptTemplate template, String input) {
        Prompt prompt = create(template, input);
        return new LlmRequest(renderer.render(prompt), prompt.outputSchema());
    }
}
