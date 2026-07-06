package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.model.GraphNodeReference;

public record RequirementKnownEvent(
    EventId eventId,
    Instant occurredAt,
    CorrelationId correlationId,
    GraphNodeReference subject,
    GraphNodeReference predicate,
    GraphNodeReference object
) implements ApplicationEvent {

    public RequirementKnownEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(predicate, "predicate must not be null");
        Objects.requireNonNull(object, "object must not be null");
    }

    public static RequirementKnownEvent create(
        CorrelationId correlationId,
        GraphNodeReference subject,
        GraphNodeReference predicate,
        GraphNodeReference object
    ) {
        return new RequirementKnownEvent(
            EventId.create(),
            Instant.now(),
            correlationId,
            subject,
            predicate,
            object
        );
    }
}
