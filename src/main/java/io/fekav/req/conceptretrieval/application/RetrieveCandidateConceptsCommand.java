package io.fekav.req.conceptretrieval.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.conceptretrieval.domain.CandidateConceptMatchSet;

public record RetrieveCandidateConceptsCommand(
    List<SelectedTermInput> selectedTerms
) implements Command<CandidateConceptMatchSet> {

    public RetrieveCandidateConceptsCommand {
        if (selectedTerms == null || selectedTerms.isEmpty()) {
            throw new IllegalArgumentException(
                "retrieve candidate concepts command has no selected terms"
            );
        }

        selectedTerms = List.copyOf(selectedTerms);
        if (selectedTerms.stream().anyMatch(java.util.Objects::isNull)) {
            throw new NullPointerException("selected term inputs must not contain null");
        }

        Set<SelectedTermInput> uniqueSelectedTerms = new HashSet<>(selectedTerms);
        if (uniqueSelectedTerms.size() != selectedTerms.size()) {
            throw new IllegalArgumentException(
                "retrieve candidate concepts command has duplicate selected terms"
            );
        }
    }

    public record SelectedTermInput(
        String syntaxRole,
        String text
    ) {

        public SelectedTermInput {
            if (syntaxRole == null || syntaxRole.isBlank()) {
                throw new IllegalArgumentException(
                    "selected term input syntax role must not be blank"
                );
            }
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("selected term input text must not be blank");
            }

            syntaxRole = syntaxRole.strip();
            text = text.strip();
        }
    }
}
