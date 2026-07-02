package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.CorrelationId;

public record ConceptMatchEvaluatedEvent(
    EventId eventId,
    Instant occurredAt,
    CorrelationId correlationId,
    CandidateConceptMatch match,
    ConceptMatchDecision decision
) implements ApplicationEvent {

    public ConceptMatchEvaluatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(match, "match must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
    }

    public static ConceptMatchEvaluatedEvent create(
        CorrelationId correlationId,
        CandidateConceptMatch match,
        ConceptMatchDecision decision
    ) {
        return new ConceptMatchEvaluatedEvent(
            EventId.create(),
            Instant.now(),
            correlationId,
            match,
            decision
        );
    }

    public static ConceptMatchEvaluatedEvent create(
        CandidateConceptMatch match,
        ConceptMatchDecision decision
    ) {
        return create(CorrelationId.create(), match, decision);
    }
}
