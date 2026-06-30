package io.fekav.req.shared.model;

public record SelectedTerm(
    String syntaxRole,
    String text
) {

    public SelectedTerm {
        if (syntaxRole == null || syntaxRole.isBlank()) {
            throw new IllegalArgumentException("selected term syntax role must not be blank");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("selected term text must not be blank");
        }

        syntaxRole = syntaxRole.strip();
        text = text.strip();
    }
}
