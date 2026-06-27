package io.fekav.req.ingestion.domain;

import java.util.List;
import java.util.Objects;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.req.shared.model.ElementId;

public record RequirementIngestion(
    ElementId id,
    SourceProvenance provenance,
    String originalText,
    String normalizedText,
    List<DomainEvent> domainEvents
) {

    public RequirementIngestion {
        id = Objects.requireNonNull(id, "id must not be null");
        provenance = Objects.requireNonNull(provenance, "provenance must not be null");
        if (originalText == null || originalText.isBlank()) {
            throw new InvalidRequirementIngestionException("original text must not be blank");
        }
        if (normalizedText == null || normalizedText.isBlank()) {
            throw new InvalidRequirementIngestionException("normalized text must not be blank");
        }

        domainEvents = List.copyOf(Objects.requireNonNull(domainEvents, "domain events must not be null"));
    }

    public static RequirementIngestion create(
        SourceProvenance provenance,
        String originalText,
        String normalizedText
    ) {
        RequirementIngestion ingestion = new RequirementIngestion(
            ElementId.create(),
            provenance,
            originalText,
            normalizedText,
            List.of()
        );

        return new RequirementIngestion(
            ingestion.id(),
            ingestion.provenance(),
            ingestion.originalText(),
            ingestion.normalizedText(),
            List.of(RequirementIngestedEvent.create(ingestion))
        );
    }
}
