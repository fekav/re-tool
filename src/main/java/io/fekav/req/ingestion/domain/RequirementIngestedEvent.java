package io.fekav.req.ingestion.domain;

import java.time.Instant;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.model.ElementId;

public record RequirementIngestedEvent(
    EventId eventId,
    Instant occurredAt,
    ElementId ingestionId,
    SourceProvenance provenance,
    String originalText,
    String normalizedText
) implements DomainEvent {

    public static RequirementIngestedEvent create(RequirementIngestion ingestion) {
        return new RequirementIngestedEvent(
            EventId.create(),
            Instant.now(),
            ingestion.id(),
            ingestion.provenance(),
            ingestion.originalText(),
            ingestion.normalizedText()
        );
    }
}
