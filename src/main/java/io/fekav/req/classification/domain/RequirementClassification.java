package io.fekav.req.classification.domain;

import java.util.Objects;

public record RequirementClassification(
    RequirementConceptType conceptType,
    RequirementProperty property,
    ConfidenceScore confidenceScore,
    ClassificationRationale rationale
) {

    public RequirementClassification {
        Objects.requireNonNull(conceptType, "conceptType must not be null");
        Objects.requireNonNull(property, "property must not be null");
        Objects.requireNonNull(confidenceScore, "confidenceScore must not be null");
        Objects.requireNonNull(rationale, "rationale must not be null");
    }
}
