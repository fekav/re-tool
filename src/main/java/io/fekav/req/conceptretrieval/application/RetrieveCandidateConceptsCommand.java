package io.fekav.req.conceptretrieval.application;

import java.util.Objects;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.shared.event.ConceptCandidatesRetrievedEvent;
import io.fekav.req.shared.model.RequirementElement;

public record RetrieveCandidateConceptsCommand(
    RequirementElement requirementElement
) implements Command<ConceptCandidatesRetrievedEvent> {

    public RetrieveCandidateConceptsCommand {
        Objects.requireNonNull(requirementElement, "selectedTerm must not be null");
    }
}
