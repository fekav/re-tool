package io.fekav.req.shared.event;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.syntaxextraction.domain.Action;

public record RequirementAnalysisCompletedEvent(
    EventId eventId,
    Instant occurredAt,
    CorrelationId correlationId,
    Provenance provenance,
    Classification classification,
    Action action,
    List<NodeMatchDecision> nodeMatchDecisions
) implements ApplicationEvent {

    public RequirementAnalysisCompletedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(provenance, "provenance must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(
            nodeMatchDecisions,
            "nodeMatchDecisions must not be null"
        );
        if (nodeMatchDecisions.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("nodeMatchDecisions must not contain null");
        }
        nodeMatchDecisions = List.copyOf(nodeMatchDecisions);
    }

    public static RequirementAnalysisCompletedEvent create(
        CorrelationId correlationId,
        Provenance provenance,
        Classification classification,
        Action action,
        List<NodeMatchDecision> nodeMatchDecisions
    ) {
        return new RequirementAnalysisCompletedEvent(
            EventId.create(),
            Instant.now(),
            correlationId,
            provenance,
            classification,
            action,
            nodeMatchDecisions
        );
    }
}
