package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.platform.messaging.CorrelationId;

public record ConceptMatchEvaluatedEvent(
    EventId eventId,
    Instant occurredAt,
    CorrelationId correlationId,
    ConceptMatchDecision decision
) implements ApplicationEvent {

    public ConceptMatchEvaluatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
    }

    public static ConceptMatchEvaluatedEvent create(
        CorrelationId correlationId,
        ConceptMatchDecision decision
    ) {
        return new ConceptMatchEvaluatedEvent(
            EventId.create(),
            Instant.now(),
            correlationId,
            decision
        );
    }

    public static ConceptMatchEvaluatedEvent create(ConceptMatchDecision decision) {
        return create(CorrelationId.create(), decision);
    }
}
