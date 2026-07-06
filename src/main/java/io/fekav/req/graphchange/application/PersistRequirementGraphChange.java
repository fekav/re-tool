package io.fekav.req.graphchange.application;

import java.util.List;
import java.util.Objects;

import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.graphchange.domain.AssertionIdentity;
import io.fekav.req.graphchange.domain.GraphQualifier;
import io.fekav.req.shared.model.Provenance;

public record PersistRequirementGraphChange(
    CorrelationId correlationId,
    String requirementId,
    Provenance provenance,
    Classification classification,
    AssertionIdentity assertionIdentity,
    List<GraphQualifier> qualifiers
) {

    public PersistRequirementGraphChange {
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        if (requirementId == null || requirementId.isBlank()) {
            throw new IllegalArgumentException("requirementId must not be blank");
        }
        Objects.requireNonNull(provenance, "provenance must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(assertionIdentity, "assertionIdentity must not be null");
        Objects.requireNonNull(qualifiers, "qualifiers must not be null");
        if (qualifiers.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("qualifiers must not contain null");
        }

        requirementId = requirementId.strip();
        qualifiers = List.copyOf(qualifiers);
    }
}
