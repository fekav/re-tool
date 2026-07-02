package io.fekav.req.shared.event;

import java.time.Instant;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.model.RawText;
import io.fekav.req.syntaxextraction.domain.Action;

public record RequirementElementsExtractedEvent(
        EventId eventId,
        Instant occurredAt,
        RawText rawText,
        Action action
) implements ApplicationEvent {

    public static RequirementElementsExtractedEvent create(
            RawText rawText,
            Action action) {
        return new RequirementElementsExtractedEvent(EventId.create(), Instant.now(), rawText, action);
    }
}
