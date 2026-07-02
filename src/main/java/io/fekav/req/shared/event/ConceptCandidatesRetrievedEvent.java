package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.platform.messaging.CorrelationId;

public record ConceptCandidatesRetrievedEvent(
    EventId eventId,
    Instant occurredAt,
    CorrelationId correlationId,
    CandidateConceptMatch match
) implements ApplicationEvent {

    public ConceptCandidatesRetrievedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(match, "match must not be null");
    }

    public static ConceptCandidatesRetrievedEvent create(
        CorrelationId correlationId,
        CandidateConceptMatch match
    ) {
        return new ConceptCandidatesRetrievedEvent(
            EventId.create(),
            Instant.now(),
            correlationId,
            match
        );
    }

    public static ConceptCandidatesRetrievedEvent create(CandidateConceptMatch match) {
        return create(CorrelationId.create(), match);
    }
}
