package io.fekav.req.resolution.application;

import io.fekav.req.resolution.domain.CandidateLookup;
import io.fekav.req.resolution.domain.ConceptNameRetrievalPolicy;
import io.fekav.req.resolution.domain.ConceptRetrievalPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ConceptRetrievalPolicyFactory {

    @Produces
    @ApplicationScoped
    ConceptRetrievalPolicy conceptRetrievalPolicy(
        CandidateLookup conceptNameLookup
    ) {
        return new ConceptNameRetrievalPolicy(conceptNameLookup);
    }
}
