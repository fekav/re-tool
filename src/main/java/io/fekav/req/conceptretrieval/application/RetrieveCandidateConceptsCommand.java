package io.fekav.req.conceptretrieval.application;

import java.util.Objects;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.shared.event.ConceptCandidatesRetrievedEvent;
import io.fekav.req.shared.model.SelectedTerm;

public record RetrieveCandidateConceptsCommand(
    SelectedTerm selectedTerm
) implements Command<ConceptCandidatesRetrievedEvent> {

    public RetrieveCandidateConceptsCommand {
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");
    }
}
