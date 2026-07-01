package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionStatus;
import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.SelectedTerm;

public record ExistingConceptProposedEvent(
    EventId eventId,
    Instant occurredAt,
    SelectedTerm selectedTerm,
    CandidateConcept existingConcept,
    String rationale
) implements ApplicationEvent {

    public ExistingConceptProposedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");
        Objects.requireNonNull(existingConcept, "existingConcept must not be null");
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("rationale must not be blank");
        }
        rationale = rationale.strip();
    }

    public static ExistingConceptProposedEvent create(ConceptMatchDecision decision) {
        Objects.requireNonNull(decision, "decision must not be null");
        if (decision.status() != ConceptMatchDecisionStatus.PROPOSE_EXISTING) {
            throw new IllegalArgumentException(
                "existing concept proposed event requires PROPOSE_EXISTING status"
            );
        }
        return new ExistingConceptProposedEvent(
            EventId.create(),
            Instant.now(),
            decision.selectedTerm(),
            decision.candidates().getFirst().candidate(),
            decision.rationale()
        );
    }
}
