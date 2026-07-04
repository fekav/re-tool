package io.fekav.req.resolution.application;

import io.fekav.req.resolution.domain.ConceptMatchingPolicy;
import io.fekav.req.resolution.domain.ThresholdConceptMatchingPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ConceptMatchingPolicyFactory {

    @Produces
    @ApplicationScoped
    ConceptMatchingPolicy conceptMatchingPolicy() {
        return new ThresholdConceptMatchingPolicy();
    }
}
