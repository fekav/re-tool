package io.fekav.req.shared.model;

import java.util.Objects;

import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;

public record ConceptMatchResult(
    CandidateConceptMatch match,
    ConceptMatchDecision decision
) {

    public ConceptMatchResult {
        Objects.requireNonNull(match, "match must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
    }
}
