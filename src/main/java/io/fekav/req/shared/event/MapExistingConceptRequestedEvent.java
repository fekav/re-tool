package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionStatus;
import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.SelectedTerm;

public record MapExistingConceptRequestedEvent(
    EventId eventId,
    Instant occurredAt,
    SelectedTerm selectedTerm,
    CandidateConcept existingConcept,
    String rationale
) implements ApplicationEvent {

    public MapExistingConceptRequestedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");
        Objects.requireNonNull(existingConcept, "existingConcept must not be null");
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("rationale must not be blank");
        }
        rationale = rationale.strip();
    }

    public static MapExistingConceptRequestedEvent create(ConceptMatchDecision decision) {
        Objects.requireNonNull(decision, "decision must not be null");
        if (decision.status() != ConceptMatchDecisionStatus.AUTO_MAP_EXISTING) {
            throw new IllegalArgumentException(
                "map existing concept requested event requires AUTO_MAP_EXISTING status"
            );
        }
        return new MapExistingConceptRequestedEvent(
            EventId.create(),
            Instant.now(),
            decision.selectedTerm(),
            decision.candidates().getFirst().candidate(),
            decision.rationale()
        );
    }
}
