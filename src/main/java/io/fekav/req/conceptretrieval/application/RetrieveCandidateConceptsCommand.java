package io.fekav.req.conceptretrieval.application;

import java.util.List;
import java.util.Objects;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.conceptretrieval.domain.CandidateConceptMatchSet;

public record RetrieveCandidateConceptsCommand(
    List<SelectedTermInput> selectedTerms
) implements Command<CandidateConceptMatchSet> {

    public RetrieveCandidateConceptsCommand {
        Objects.requireNonNull(
            selectedTerms,
            "Retrieve candidate concepts command selected terms must not be null"
        );
        selectedTerms.forEach(term ->
            Objects.requireNonNull(term, "selected term input must not be null")
        );
        if (selectedTerms.isEmpty()) {
            throw new IllegalArgumentException(
                "Retrieve candidate concepts command has no selected terms"
            );
        }

        selectedTerms = List.copyOf(selectedTerms);
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
                throw new IllegalArgumentException(
                    "selected term input text must not be blank"
                );
            }

            syntaxRole = syntaxRole.strip();
            text = text.strip();
        }
    }
}
