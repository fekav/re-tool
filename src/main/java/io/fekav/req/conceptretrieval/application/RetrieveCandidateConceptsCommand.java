package io.fekav.req.conceptretrieval.application;

import java.util.Objects;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.shared.event.ConceptCandidatesRetrievedEvent;
import io.fekav.req.shared.model.CorrelationId;
import io.fekav.req.shared.model.RequirementElement;

public record RetrieveCandidateConceptsCommand(
    CorrelationId correlationId,
    RequirementElement requirementElement
) implements Command<ConceptCandidatesRetrievedEvent> {

    public RetrieveCandidateConceptsCommand {
        correlationId = correlationId == null
            ? CorrelationId.create()
            : correlationId;
        Objects.requireNonNull(requirementElement, "selectedTerm must not be null");
    }

    public RetrieveCandidateConceptsCommand(RequirementElement requirementElement) {
        this(CorrelationId.create(), requirementElement);
    }
}
