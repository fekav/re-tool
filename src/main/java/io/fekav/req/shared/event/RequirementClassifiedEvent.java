package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.model.CorrelationId;
import io.fekav.req.shared.model.RawText;

public record RequirementClassifiedEvent(
    EventId eventId,
    Instant occurredAt,
    CorrelationId correlationId,
    RawText rawText,
    Classification classification
) implements ApplicationEvent {

    public RequirementClassifiedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(rawText, "rawText must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
    }

    public static RequirementClassifiedEvent create(
        CorrelationId correlationId,
        RawText rawText,
        Classification classification
    ) {
        return new RequirementClassifiedEvent(
            EventId.create(),
            Instant.now(),
            correlationId,
            rawText,
            classification
        );
    }

    public static RequirementClassifiedEvent create(
        RawText rawText,
        Classification classification
    ) {
        return create(CorrelationId.create(), rawText, classification);
    }
}
