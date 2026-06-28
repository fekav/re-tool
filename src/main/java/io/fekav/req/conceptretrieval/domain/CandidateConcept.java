package io.fekav.req.conceptretrieval.domain;

public record CandidateConcept(
    String conceptId,
    String label,
    String conceptType
) {

    public CandidateConcept {
        if (conceptId == null || conceptId.isBlank()) {
            throw new IllegalArgumentException(
                "candidate concept id must not be blank"
            );
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException(
                "candidate concept label must not be blank"
            );
        }

        conceptId = conceptId.strip();
        label = label.strip();
        conceptType = conceptType == null || conceptType.isBlank()
            ? null
            : conceptType.strip();
    }
}
