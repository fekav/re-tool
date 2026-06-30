package io.fekav.req.shared.model;

import java.util.Objects;

public record SelectedTerm(
    RequirementElement requirementElement,
    String text
) {

    public SelectedTerm {
        Objects.requireNonNull(
            requirementElement,
            "selected term requirement element must not be null"
        );
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("selected term text must not be blank");
        }

        text = text.strip();
    }
}
