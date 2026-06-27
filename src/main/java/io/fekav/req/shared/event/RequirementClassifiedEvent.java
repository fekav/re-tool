package io.fekav.req.shared.event;

import java.time.Instant;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.model.ElementId;

public record RequirementClassifiedEvent(
    EventId eventId,
    Instant occurredAt,
    ElementId requirementId,
    Classification classification
) implements DomainEvent {

    public static RequirementClassifiedEvent create(
        ElementId id,
        Classification classification
    ) {
        return new RequirementClassifiedEvent(EventId.create(), Instant.now(), id, classification);
    }
}
