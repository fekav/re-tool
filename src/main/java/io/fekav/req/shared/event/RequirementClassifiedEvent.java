package io.fekav.req.shared.event;

import java.time.Instant;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.model.RawText;

public record RequirementClassifiedEvent(
        EventId eventId,
        Instant occurredAt,
        RawText rawText,
        Classification classification) implements DomainEvent {

    public static RequirementClassifiedEvent create(
            RawText rawText,
            Classification classification) {
        return new RequirementClassifiedEvent(
            EventId.create(),
            Instant.now(),
            rawText,
            classification
        );
    }
}
