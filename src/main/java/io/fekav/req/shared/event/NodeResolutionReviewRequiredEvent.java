package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.model.NodeMatchReviewRequest;

public record NodeResolutionReviewRequiredEvent(
    EventId eventId,
    Instant occurredAt,
    CorrelationId correlationId,
    NodeMatchReviewRequest reviewRequest
) implements NodeResolutionEvent {

    public NodeResolutionReviewRequiredEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(reviewRequest, "reviewRequest must not be null");
    }

    public static NodeResolutionReviewRequiredEvent create(
        CorrelationId correlationId,
        NodeMatchReviewRequest reviewRequest
    ) {
        return new NodeResolutionReviewRequiredEvent(
            EventId.create(),
            Instant.now(),
            correlationId,
            reviewRequest
        );
    }

    public static NodeResolutionReviewRequiredEvent create(
        NodeMatchReviewRequest reviewRequest
    ) {
        return create(CorrelationId.create(), reviewRequest);
    }
}
