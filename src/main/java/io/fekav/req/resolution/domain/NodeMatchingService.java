package io.fekav.req.resolution.domain;

import java.util.Objects;

import io.fekav.req.shared.model.CandidateNodeMatch;

public class NodeMatchingService {

    private final NodeMatchingPolicy matchingPolicy;

    public NodeMatchingService(NodeMatchingPolicy matchingPolicy) {
        this.matchingPolicy = Objects.requireNonNull(
            matchingPolicy,
            "matchingPolicy must not be null"
        );
    }

    public NodeMatchingResult evaluateMatch(CandidateNodeMatch match) {
        Objects.requireNonNull(match, "match must not be null");
        NodeMatchingResult result = Objects.requireNonNull(
            matchingPolicy.decide(match),
            "matching policy result must not be null"
        );
        if (!match.requirementElement().equals(result.requirementElement())) {
            throw new IllegalStateException(
                "matching policy returned a result for a different requirement element"
            );
        }
        return result;
    }
}
