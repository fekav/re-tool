package io.fekav.req.entityextraction;

import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.LlmRequest;
import io.fekav.platform.llm.PromptFactory;
import io.fekav.platform.llm.PromptTemplate;
import io.fekav.req.entityextraction.ai.ExtractionResponseParser;
import io.fekav.req.entityextraction.model.RequirementSyntax;
import io.fekav.req.shared.model.Requirement;

/**
 * Domain Service
 */
public class EntityExtractionService {

    private final LlmClientPort llmPort;
    private final PromptFactory promptFactory;
    private final PromptTemplate extractionPromptTemplate;
    private final ExtractionResponseParser parser;

    public EntityExtractionService(
            LlmClientPort llmPort,
            PromptFactory promptFactory,
            PromptTemplate extractionPromptTemplate,
            ExtractionResponseParser parser) {
        this.llmPort = llmPort;
        this.promptFactory = promptFactory;
        this.extractionPromptTemplate = extractionPromptTemplate;
        this.parser = parser;
    }

    public RequirementSyntax extract(Requirement requirement) {
        // 1. Build extraction request
        LlmRequest request = promptFactory.createRequest(extractionPromptTemplate, requirement.getRawText());
        // 2. Run extraction inference
        String extractionResponse = llmPort.generate(request);
        // 3. Parse inference response
        RequirementSyntax extractionResult = parser.parse(extractionResponse);

        return extractionResult;
    }

}
