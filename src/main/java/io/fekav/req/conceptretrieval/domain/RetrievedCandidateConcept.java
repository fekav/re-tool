package io.fekav.req.conceptretrieval.domain;

import java.util.List;
import java.util.Objects;

public record RetrievedCandidateConcept(
    CandidateConcept candidate,
    List<RetrievalEvidence> evidence
) {

    public RetrievedCandidateConcept {
        Objects.requireNonNull(candidate, "candidate must not be null");
        if (evidence == null || evidence.isEmpty()) {
            throw new IllegalArgumentException("retrieved candidate evidence must not be empty");
        }
        evidence = List.copyOf(evidence);
        if (evidence.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("retrieved candidate evidence must not contain null");
        }
    }
}
