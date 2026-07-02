package io.fekav.req.shared.model;

public record OriginalText(String text) {

    public OriginalText {
        if (text == null || text.isBlank()) {
            throw new InvalidOriginalTextException();
        }
    }
}
