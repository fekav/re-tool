package io.fekav.req;

import io.fekav.platform.llm.LlmClientPort;
import io.fekav.req.entityextraction.EntityExtractionService;
import io.fekav.req.entityextraction.ai.ExtractionPromptFactory;
import io.fekav.req.entityextraction.ai.ExtractionResponseParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class DomainServiceProducer {
 
    @Inject
    LlmClientPort llmClientPort;

    @Inject
    ExtractionPromptFactory extractionPromptFactory;

    @Inject
    ExtractionResponseParser extractionResponseParser;
     
    @Produces
    @ApplicationScoped
    public EntityExtractionService extractionService() {
        return new EntityExtractionService(llmClientPort, extractionPromptFactory, extractionResponseParser);
    }
}