package io.fekav.req.resolution.application;

import io.fekav.req.resolution.domain.CandidateLookup;
import io.fekav.req.resolution.domain.NodeNameRetrievalPolicy;
import io.fekav.req.resolution.domain.NodeRetrievalPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class NodeRetrievalPolicyFactory {

    @Produces
    @ApplicationScoped
    NodeRetrievalPolicy nodeRetrievalPolicy(
        CandidateLookup nodeNameLookup
    ) {
        return new NodeNameRetrievalPolicy(nodeNameLookup);
    }
}
