package io.fekav.req.syntaxextraction.application;

public record SyntaxElementsResponse(
    String SUBJECT,
    String ACTION,
    String OBJECT,
    String CONSTRAINT,
    String CONDITION
) {
}
