package io.fekav.req.conceptretrieval.application;

import io.fekav.req.conceptretrieval.domain.CandidateLookup;
import io.fekav.req.conceptretrieval.domain.ConceptNameRetrievalPolicy;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalPolicy;
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
