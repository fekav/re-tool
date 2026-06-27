package io.fekav.req.syntaxextraction.domain;

import java.util.Objects;

import io.fekav.req.shared.model.ElementId;

public record Subject(
    ElementId id,
    String text
) {

    public Subject {
        Objects.requireNonNull(id, "id must not be null");
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("subject text must not be blank");
        }

        text = text.strip();
    }
}
