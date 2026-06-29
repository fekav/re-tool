package io.fekav.req.conceptretrieval.application;

import java.util.List;

import io.fekav.req.conceptretrieval.domain.ConceptRetrievalService;
import io.fekav.req.conceptretrieval.domain.OrderedWeightedConceptRetrievalPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ConceptRetrievalServiceFactory {

    @Produces
    @ApplicationScoped
    ConceptRetrievalService conceptRetrievalService(
        @Any Instance<ConceptCandidateLookup> candidateLookups
    ) {
        List<ConceptCandidateLookup> lookups = candidateLookups.stream().toList();
        return new ConceptRetrievalService(
            new OrderedWeightedConceptRetrievalPolicy(lookups)
        );
    }
}
