package io.fekav.req.resolution.application;

import java.util.List;

import io.fekav.req.resolution.domain.CandidateLookup;
import io.fekav.req.resolution.domain.CompatibleCandidateLookup;
import io.fekav.req.resolution.domain.EvidenceBasedNodeRetrievalPolicy;
import io.fekav.req.resolution.domain.NodeNameRetrievalPolicy;
import io.fekav.req.resolution.domain.NodeRetrievalPolicy;
import io.fekav.req.resolution.domain.TokenOverlapNodeRetrievalPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class NodeRetrievalPolicyFactory {

    @Produces
    @ApplicationScoped
    NodeRetrievalPolicy nodeRetrievalPolicy(
        CandidateLookup nodeNameLookup,
        CompatibleCandidateLookup compatibleCandidateLookup
    ) {
        NodeRetrievalPolicy exactPolicy =
            new NodeNameRetrievalPolicy(nodeNameLookup);
        NodeRetrievalPolicy tokenOverlapPolicy =
            new TokenOverlapNodeRetrievalPolicy(compatibleCandidateLookup);
        return new EvidenceBasedNodeRetrievalPolicy(List.of(
            exactPolicy,
            tokenOverlapPolicy
        ));
    }
}
