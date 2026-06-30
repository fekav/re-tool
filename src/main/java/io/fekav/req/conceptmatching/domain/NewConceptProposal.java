package io.fekav.req.conceptmatching.domain;

import java.util.Objects;

import io.fekav.req.shared.model.RequirementElement;

public record NewConceptProposal(
    String label,
    RequirementElement requirementElement
) {

    public NewConceptProposal {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("new concept proposal label must not be blank");
        }
        Objects.requireNonNull(
            requirementElement,
            "new concept proposal requirement element must not be null"
        );

        label = label.strip();
    }
}
