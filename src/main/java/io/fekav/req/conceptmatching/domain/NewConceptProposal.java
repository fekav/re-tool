package io.fekav.req.conceptmatching.domain;

public record NewConceptProposal(
    String label,
    String conceptType
) {

    public NewConceptProposal {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("new concept proposal label must not be blank");
        }
        if (conceptType == null || conceptType.isBlank()) {
            throw new IllegalArgumentException(
                "new concept proposal concept type must not be blank"
            );
        }

        label = label.strip();
        conceptType = conceptType.strip();
    }
}
