package io.fekav.req.conceptretrieval.domain;

import java.util.Objects;

public record CandidateLookupHit(
    CandidateConcept candidate,
    String lookupMethodName,
    String evidenceText
) {

    public CandidateLookupHit {
        Objects.requireNonNull(candidate, "candidate must not be null");
        if (lookupMethodName == null || lookupMethodName.isBlank()) {
            throw new IllegalArgumentException("candidate lookup hit method name must not be blank");
        }
        if (evidenceText == null || evidenceText.isBlank()) {
            throw new IllegalArgumentException("candidate lookup hit evidence text must not be blank");
        }

        lookupMethodName = lookupMethodName.strip();
        evidenceText = evidenceText.strip();
    }
}
