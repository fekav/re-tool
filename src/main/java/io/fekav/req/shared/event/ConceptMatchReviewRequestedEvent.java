package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionStatus;
import io.fekav.req.shared.model.SelectedTerm;

public record ConceptMatchReviewRequestedEvent(
    EventId eventId,
    Instant occurredAt,
    SelectedTerm selectedTerm,
    String rationale
) implements ApplicationEvent {

    public ConceptMatchReviewRequestedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("rationale must not be blank");
        }
        rationale = rationale.strip();
    }

    public static ConceptMatchReviewRequestedEvent create(ConceptMatchDecision decision) {
        Objects.requireNonNull(decision, "decision must not be null");
        if (decision.status() != ConceptMatchDecisionStatus.REVIEW_REQUIRED) {
            throw new IllegalArgumentException(
                "concept match review requested event requires REVIEW_REQUIRED status"
            );
        }
        return new ConceptMatchReviewRequestedEvent(
            EventId.create(),
            Instant.now(),
            decision.selectedTerm(),
            decision.rationale()
        );
    }
}
