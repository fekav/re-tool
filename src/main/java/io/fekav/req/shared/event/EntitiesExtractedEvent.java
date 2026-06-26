package io.fekav.req.shared.event;

import java.time.Instant;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.model.RequirementId;
import io.fekav.req.syntaxextraction.domain.RequirementSyntax;

/**
 * Domain event
 */
public record EntitiesExtractedEvent(

        EventId eventId,

        Instant occurredAt,

        RequirementId requirementId,

        RequirementSyntax syntaxElements

) implements DomainEvent {
    
    public static EntitiesExtractedEvent create(
            RequirementId id,
            RequirementSyntax  syntaxElements) {
        return new EntitiesExtractedEvent(EventId.create(), Instant.now(), id, syntaxElements);
    }
}