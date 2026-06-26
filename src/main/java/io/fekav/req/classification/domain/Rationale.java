package io.fekav.req.classification.domain;

public record Rationale(String text) {

    public Rationale {
        if (text == null || text.isBlank()) {
            throw new InvalidClassificationRationaleException();
        }

        text = text.strip();
    }
}
