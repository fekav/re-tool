package io.fekav.req.conceptmatching.domain;

import java.util.List;
import java.util.Objects;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.CandidateConceptMatchSet;

public class ConceptMatchingService {

    private final ConceptMatchingPolicy matchingPolicy;

    public ConceptMatchingService(ConceptMatchingPolicy matchingPolicy) {
        this.matchingPolicy = Objects.requireNonNull(
            matchingPolicy,
            "matchingPolicy must not be null"
        );
    }

    public ConceptMatchDecisionSet decideMatches(CandidateConceptMatchSet matches) {
        Objects.requireNonNull(matches, "matches must not be null");

        List<ConceptMatchDecision> decisions = matches
            .matches()
            .stream()
            .map(this::decideMatch)
            .toList();

        return new ConceptMatchDecisionSet(decisions);
    }

    private ConceptMatchDecision decideMatch(CandidateConceptMatch match) {
        ConceptMatchDecision decision = matchingPolicy.decide(match);
        if (!match.selectedTerm().equals(decision.selectedTerm())) {
            throw new IllegalStateException(
                "matching policy returned a decision for a different selected term"
            );
        }
        return decision;
    }
}
