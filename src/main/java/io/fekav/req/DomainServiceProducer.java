package io.fekav.req;

import io.fekav.platform.llm.LlmClientPort;
import io.fekav.platform.llm.PromptFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DomainServiceProducer {
 
    @Inject
    LlmClientPort llmClientPort;

    @Inject
    PromptFactory promptFactory;

}
