package io.fekav.req;

import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.PromptFactory;
import io.fekav.req.entityextraction.EntityExtractionService;
import io.fekav.req.entityextraction.ai.ExtractionPromptTemplate;
import io.fekav.req.entityextraction.ai.ExtractionResponseParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class DomainServiceProducer {
 
    @Inject
    LlmClientPort llmClientPort;

    @Inject
    PromptFactory promptFactory;

    @Inject
    ExtractionPromptTemplate extractionPromptTemplate;

    @Inject
    ExtractionResponseParser extractionResponseParser;
     
    @Produces
    @ApplicationScoped
    public EntityExtractionService extractionService() {
        return new EntityExtractionService(
                llmClientPort,
                promptFactory,
                extractionPromptTemplate.template(),
                extractionResponseParser);
    }
}
