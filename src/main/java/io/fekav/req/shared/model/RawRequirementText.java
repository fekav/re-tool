package io.fekav.req.shared.model;

public record RawRequirementText(String text) {

    public RawRequirementText {
        if (text == null || text.isBlank()) {
            throw new InvalidRawRequirementTextException();
        }

        text = text.strip();
    }
}
