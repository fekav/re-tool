package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.model.CandidateConceptMatch;

public record ConceptCandidatesReadyEvent(
    EventId eventId,
    Instant occurredAt,
    CandidateConceptMatch match
) implements ApplicationEvent {

    public ConceptCandidatesReadyEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(match, "match must not be null");
    }

    public static ConceptCandidatesReadyEvent create(CandidateConceptMatch match) {
        return new ConceptCandidatesReadyEvent(EventId.create(), Instant.now(), match);
    }
}
