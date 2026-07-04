package io.fekav.req.resolution.domain;

import java.util.Objects;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.ConceptMatchDecision;

public class ConceptMatchingService {

    private final ConceptMatchingPolicy matchingPolicy;

    public ConceptMatchingService(ConceptMatchingPolicy matchingPolicy) {
        this.matchingPolicy = Objects.requireNonNull(
            matchingPolicy,
            "matchingPolicy must not be null"
        );
    }

    public ConceptMatchDecision evaluateMatch(CandidateConceptMatch match) {
        Objects.requireNonNull(match, "match must not be null");
        ConceptMatchDecision decision = matchingPolicy.decide(match);
        if (!match.requirementElement().equals(decision.requirementElement())) {
            throw new IllegalStateException(
                "matching policy returned a decision for a different requirement element"
            );
        }
        return decision;
    }
}
