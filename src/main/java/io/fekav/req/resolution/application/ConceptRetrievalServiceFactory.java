package io.fekav.req.resolution.application;

import io.fekav.req.resolution.domain.ConceptRetrievalPolicy;
import io.fekav.req.resolution.domain.ConceptRetrievalService;
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
