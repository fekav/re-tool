package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.model.NodeMatchDecision;

public record NodeResolutionDecidedEvent(
    EventId eventId,
    Instant occurredAt,
    CorrelationId correlationId,
    NodeMatchDecision decision
) implements ApplicationEvent {

    public NodeResolutionDecidedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
    }

    public static NodeResolutionDecidedEvent create(
        CorrelationId correlationId,
        NodeMatchDecision decision
    ) {
        return new NodeResolutionDecidedEvent(
            EventId.create(),
            Instant.now(),
            correlationId,
            decision
        );
    }

    public static NodeResolutionDecidedEvent create(NodeMatchDecision decision) {
        return create(CorrelationId.create(), decision);
    }
}
