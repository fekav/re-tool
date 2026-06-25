package io.fekav.req.classification.infrastructure;

public record RequirementClassificationFieldsOutput(
    String conceptType,
    String property,
    Double confidenceScore,
    String rationale
) {
}
