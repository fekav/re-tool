package io.fekav.req.syntaxextraction.infrastructure;

public record SyntaxElementsOutput(
    String SUBJECT,
    String ACTION,
    String OBJECT,
    String CONSTRAINT,
    String CONDITION
) {
}
