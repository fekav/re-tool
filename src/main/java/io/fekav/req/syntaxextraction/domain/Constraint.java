package io.fekav.req.syntaxextraction.domain;

public record Constraint(String text) {

    public Constraint {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("constraint text must not be blank");
        }

        text = text.strip();
    }
}
