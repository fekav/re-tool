package io.fekav.req.conceptmatching.application;

import io.fekav.req.conceptmatching.domain.ConceptMatchingPolicy;
import io.fekav.req.conceptmatching.domain.ThresholdConceptMatchingPolicy;
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
