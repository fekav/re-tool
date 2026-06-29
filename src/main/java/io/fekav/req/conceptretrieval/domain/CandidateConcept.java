package io.fekav.req.conceptretrieval.domain;

public record CandidateConcept(
    String candidateKey,
    String label,
    String conceptType
) {

    public CandidateConcept {
        if (candidateKey == null || candidateKey.isBlank()) {
            throw new IllegalArgumentException("candidate concept key must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("candidate concept label must not be blank");
        }

        candidateKey = candidateKey.strip();
        label = label.strip();
        conceptType = conceptType == null || conceptType.isBlank()
            ? null
            : conceptType.strip();
    }
}
