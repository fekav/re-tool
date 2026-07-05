package io.fekav.req.resolution.application;

import io.fekav.req.resolution.domain.NodeMatchingPolicy;
import io.fekav.req.resolution.domain.NodeMatchingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class NodeMatchingServiceFactory {

    @Produces
    @ApplicationScoped
    NodeMatchingService nodeMatchingService(
        NodeMatchingPolicy matchingPolicy
    ) {
        return new NodeMatchingService(matchingPolicy);
    }
}
