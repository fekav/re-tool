package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.model.CorrelationId;
import io.fekav.req.shared.model.Provenance;

public record RequirementIngestedEvent(
    EventId eventId,
    Instant occurredAt,
    CorrelationId correlationId,
    Provenance provenance
) implements ApplicationEvent {

    public RequirementIngestedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(provenance, "provenance must not be null");
    }

    public static RequirementIngestedEvent create(Provenance provenance) {
        return new RequirementIngestedEvent(
            EventId.create(),
            provenance.getIngestedAt(),
            CorrelationId.create(),
            provenance
        );
    }
}
