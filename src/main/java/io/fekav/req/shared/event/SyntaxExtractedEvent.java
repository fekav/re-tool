package io.fekav.req.shared.event;

import java.time.Instant;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.syntaxextraction.domain.Action;

/**
 * Domain event
 */
public record SyntaxExtractedEvent(

        EventId eventId,

        Instant occurredAt,

        ElementId requirementId,

        Action action

) implements DomainEvent {
    
    public static SyntaxExtractedEvent create(
            ElementId id,
            Action action) {
        return new SyntaxExtractedEvent(EventId.create(), Instant.now(), id, action);
    }
}
