package io.fekav.req.conceptmatching.application;

import java.util.Objects;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.shared.event.ConceptMatchEvaluatedEvent;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.platform.messaging.CorrelationId;

public record EvaluateConceptMatchCommand(
    CorrelationId correlationId,
    CandidateConceptMatch match
) implements Command<ConceptMatchEvaluatedEvent> {

    public EvaluateConceptMatchCommand {
        correlationId = correlationId == null
            ? CorrelationId.create()
            : correlationId;
        Objects.requireNonNull(match, "match must not be null");
    }

    public EvaluateConceptMatchCommand(CandidateConceptMatch match) {
        this(CorrelationId.create(), match);
    }
}
