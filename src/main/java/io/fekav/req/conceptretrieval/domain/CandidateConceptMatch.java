package io.fekav.req.conceptretrieval.domain;

import java.util.List;
import java.util.Objects;

public record CandidateConceptMatch(
    String syntaxRole,
    String text,
    List<CandidateConcept> candidates,
    List<RetrievalEvidence> evidence
) {

    public CandidateConceptMatch {
        if (syntaxRole == null || syntaxRole.isBlank()) {
            throw new IllegalArgumentException(
                "candidate concept match syntax role must not be blank"
            );
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                "candidate concept match text must not be blank"
            );
        }
        Objects.requireNonNull(candidates, "candidate concepts must not be null");
        Objects.requireNonNull(evidence, "candidate concept match evidence must not be null");
        candidates.forEach(candidate ->
            Objects.requireNonNull(candidate, "candidate concept must not be null")
        );
        evidence.forEach(evidenceItem ->
            Objects.requireNonNull(evidenceItem, "retrieval evidence must not be null")
        );
        if (evidence.isEmpty()) {
            throw new IllegalArgumentException(
                "candidate concept match evidence must not be empty"
            );
        }

        syntaxRole = syntaxRole.strip();
        text = text.strip();
        candidates = List.copyOf(candidates);
        evidence = List.copyOf(evidence);
    }
}
