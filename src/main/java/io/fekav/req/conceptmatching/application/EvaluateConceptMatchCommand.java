package io.fekav.req.conceptmatching.application;

import java.util.Objects;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.shared.event.ConceptMatchEvaluatedEvent;
import io.fekav.req.shared.model.CandidateConceptMatch;

public record EvaluateConceptMatchCommand(
    CandidateConceptMatch match
) implements Command<ConceptMatchEvaluatedEvent> {

    public EvaluateConceptMatchCommand {
        Objects.requireNonNull(match, "match must not be null");
    }
}
