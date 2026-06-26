package io.fekav.req.classification.domain;

import java.util.Objects;

public record Classification(
    RequirementType conceptType,
    RequirementProperty property,
    ConfidenceScore confidenceScore,
    Rationale rationale
) {

    public Classification {
        Objects.requireNonNull(conceptType, "conceptType must not be null");
        Objects.requireNonNull(property, "property must not be null");
        Objects.requireNonNull(confidenceScore, "confidenceScore must not be null");
        Objects.requireNonNull(rationale, "rationale must not be null");
    }
}
