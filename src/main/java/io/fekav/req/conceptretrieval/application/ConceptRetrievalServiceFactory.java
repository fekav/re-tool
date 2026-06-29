package io.fekav.req.conceptretrieval.application;

import io.fekav.req.conceptretrieval.domain.ConceptRetrievalPolicy;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ConceptRetrievalServiceFactory {

    @Produces
    @ApplicationScoped
    ConceptRetrievalService conceptRetrievalService(
        ConceptRetrievalPolicy retrievalPolicy
    ) {
        return new ConceptRetrievalService(retrievalPolicy);
    }
}
