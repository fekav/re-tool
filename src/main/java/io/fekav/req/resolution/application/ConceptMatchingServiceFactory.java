package io.fekav.req.resolution.application;

import io.fekav.req.resolution.domain.ConceptMatchingPolicy;
import io.fekav.req.resolution.domain.ConceptMatchingService;
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
