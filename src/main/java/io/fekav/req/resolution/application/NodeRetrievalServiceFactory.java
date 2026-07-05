package io.fekav.req.resolution.application;

import io.fekav.req.resolution.domain.NodeRetrievalPolicy;
import io.fekav.req.resolution.domain.NodeRetrievalService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class NodeRetrievalServiceFactory {

    @Produces
    @ApplicationScoped
    NodeRetrievalService nodeRetrievalService(
        NodeRetrievalPolicy retrievalPolicy
    ) {
        return new NodeRetrievalService(retrievalPolicy);
    }
}
