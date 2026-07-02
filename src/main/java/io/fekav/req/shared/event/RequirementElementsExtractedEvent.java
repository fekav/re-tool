package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.model.CorrelationId;
import io.fekav.req.shared.model.RawText;
import io.fekav.req.syntaxextraction.domain.Action;

public record RequirementElementsExtractedEvent(
    EventId eventId,
    Instant occurredAt,
    CorrelationId correlationId,
    RawText rawText,
    Action action
) implements ApplicationEvent {

    public RequirementElementsExtractedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(rawText, "rawText must not be null");
        Objects.requireNonNull(action, "action must not be null");
    }

    public static RequirementElementsExtractedEvent create(
        CorrelationId correlationId,
        RawText rawText,
        Action action
    ) {
        return new RequirementElementsExtractedEvent(
            EventId.create(),
            Instant.now(),
            correlationId,
            rawText,
            action
        );
    }

    public static RequirementElementsExtractedEvent create(
        RawText rawText,
        Action action
    ) {
        return create(CorrelationId.create(), rawText, action);
    }
}
