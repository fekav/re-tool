package io.fekav.req.resolution.application;

import io.fekav.req.resolution.domain.NodeMatchingPolicy;
import io.fekav.req.resolution.domain.ThresholdNodeMatchingPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class NodeMatchingPolicyFactory {

    @Produces
    @ApplicationScoped
    NodeMatchingPolicy nodeMatchingPolicy() {
        return new ThresholdNodeMatchingPolicy();
    }
}
