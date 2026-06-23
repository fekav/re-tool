package io.fekav.req.entityextraction;

import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.LlmRequest;
import io.fekav.req.entityextraction.ai.ExtractionPrompt;
import io.fekav.req.entityextraction.ai.ExtractionPromptFactory;
import io.fekav.req.entityextraction.ai.ExtractionResponseParser;
import io.fekav.req.entityextraction.model.RequirementSyntax;
import io.fekav.req.shared.model.Requirement;

/**
 * Domain Service
 */
public class EntityExtractionService {

    private final LlmClientPort llmPort;
    private final ExtractionPromptFactory promptFactory;
    private final ExtractionResponseParser parser;

    public EntityExtractionService(
            LlmClientPort llmPort,
            ExtractionPromptFactory promptFactory,
            ExtractionResponseParser parser) {
        this.llmPort = llmPort;
        this.promptFactory = promptFactory;
        this.parser = parser;
    }

    public RequirementSyntax extract(Requirement requirement) {
        // 1. Build prompt
        ExtractionPrompt prompt = promptFactory.create(requirement.getRawText());
        // 2. extraction inference
        String extractionResponse = llmPort.generate(
            new LlmRequest(prompt.toString(), prompt.getOutputSchema())
        );
        // 3. parse inference response       
        RequirementSyntax extractionResult = parser.parse(extractionResponse);

        return extractionResult;
    }

}
