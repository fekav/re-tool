package io.fekav.req.resolution.domain;

import java.util.Objects;

import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.NodeMatchDecision;

public class NodeMatchingService {

    private final NodeMatchingPolicy matchingPolicy;

    public NodeMatchingService(NodeMatchingPolicy matchingPolicy) {
        this.matchingPolicy = Objects.requireNonNull(
            matchingPolicy,
            "matchingPolicy must not be null"
        );
    }

    public NodeMatchDecision evaluateMatch(CandidateNodeMatch match) {
        Objects.requireNonNull(match, "match must not be null");
        NodeMatchDecision decision = matchingPolicy.decide(match);
        if (!match.requirementElement().equals(decision.requirementElement())) {
            throw new IllegalStateException(
                "matching policy returned a decision for a different requirement element"
            );
        }
        return decision;
    }
}
