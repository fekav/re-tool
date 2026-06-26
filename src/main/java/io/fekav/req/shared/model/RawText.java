package io.fekav.req.shared.model;

public record RawText(String text) {

    public RawText {
        if (text == null || text.isBlank()) {
            throw new InvalidRawRequirementTextException();
        }

        text = text.strip();
    }
}
