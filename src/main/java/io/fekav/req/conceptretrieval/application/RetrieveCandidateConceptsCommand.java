package io.fekav.req.conceptretrieval.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.shared.model.CandidateConceptMatchSet;
import io.fekav.req.shared.model.SelectedTerm;

public record RetrieveCandidateConceptsCommand(
    List<SelectedTerm> selectedTerms
) implements Command<CandidateConceptMatchSet> {

    public RetrieveCandidateConceptsCommand {
        if (selectedTerms == null || selectedTerms.isEmpty()) {
            throw new IllegalArgumentException(
                "retrieve candidate concepts command has no selected terms"
            );
        }

        selectedTerms = List.copyOf(selectedTerms);
        if (selectedTerms.stream().anyMatch(java.util.Objects::isNull)) {
            throw new NullPointerException("selected terms must not contain null");
        }

        Set<SelectedTerm> uniqueSelectedTerms = new HashSet<>(selectedTerms);
        if (uniqueSelectedTerms.size() != selectedTerms.size()) {
            throw new IllegalArgumentException(
                "retrieve candidate concepts command has duplicate selected terms"
            );
        }
    }
}
