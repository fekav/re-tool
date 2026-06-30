package io.fekav.req.shared.event;

import java.time.Instant;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.model.RequirementId;

public record RequirementClassifiedEvent(
        EventId eventId,
        Instant occurredAt,
        RequirementId requirementId,
        Classification classification) implements DomainEvent {

    public static RequirementClassifiedEvent create(
            RequirementId id,
            Classification classification) {
        return new RequirementClassifiedEvent(EventId.create(), Instant.now(), id, classification);
    }
}
