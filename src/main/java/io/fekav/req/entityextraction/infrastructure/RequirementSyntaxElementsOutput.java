package io.fekav.req.entityextraction.infrastructure;

public record RequirementSyntaxElementsOutput(
    String SUBJECT,
    String ACTION,
    String OBJECT,
    String CONSTRAINT,
    String CONDITION
) {
}
