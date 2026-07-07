package io.fekav.req.extraction.infrastructure;

public record RequirementElementsOutput(
    String SUBJECT,
    String ACTION,
    String OBJECT,
    String CONSTRAINT,
    String CONDITION
) {
}
