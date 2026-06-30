package io.fekav.req.conceptmatching.application;

import io.fekav.req.conceptmatching.domain.ConceptMatchingPolicy;
import io.fekav.req.conceptmatching.domain.ConceptMatchingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ConceptMatchingServiceFactory {

    @Produces
    @ApplicationScoped
    ConceptMatchingService conceptMatchingService(
        ConceptMatchingPolicy matchingPolicy
    ) {
        return new ConceptMatchingService(matchingPolicy);
    }
}
