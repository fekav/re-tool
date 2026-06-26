package io.fekav.req.syntaxextraction.infrastructure;

public record RequirementSyntaxElementsOutput(
    String SUBJECT,
    String ACTION,
    String OBJECT,
    String CONSTRAINT,
    String CONDITION
) {
}
