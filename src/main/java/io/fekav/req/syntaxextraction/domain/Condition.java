package io.fekav.req.syntaxextraction.domain;

public record Condition(String text) {

    public Condition {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("condition text must not be blank");
        }

        text = text.strip();
    }
}
