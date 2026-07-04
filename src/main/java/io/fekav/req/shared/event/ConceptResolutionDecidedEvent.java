package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.model.ConceptMatchDecision;

public record ConceptResolutionDecidedEvent(
    EventId eventId,
    Instant occurredAt,
    CorrelationId correlationId,
    ConceptMatchDecision decision
) implements ApplicationEvent {

    public ConceptResolutionDecidedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
    }

    public static ConceptResolutionDecidedEvent create(
        CorrelationId correlationId,
        ConceptMatchDecision decision
    ) {
        return new ConceptResolutionDecidedEvent(
            EventId.create(),
            Instant.now(),
            correlationId,
            decision
        );
    }

    public static ConceptResolutionDecidedEvent create(ConceptMatchDecision decision) {
        return create(CorrelationId.create(), decision);
    }
}
