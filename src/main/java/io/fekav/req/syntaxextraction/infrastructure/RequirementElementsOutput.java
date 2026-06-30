package io.fekav.req.syntaxextraction.infrastructure;

public record RequirementElementsOutput(
    String SUBJECT,
    String ACTION,
    String OBJECT,
    String CONSTRAINT,
    String CONDITION
) {
}
