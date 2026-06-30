package io.fekav.req.conceptmatching.application;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionSet;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.SelectedTerm;

public record DecideConceptMatchesCommand(
    List<CandidateConceptMatch> matches
) implements Command<ConceptMatchDecisionSet> {

    public DecideConceptMatchesCommand {
        if (matches == null || matches.isEmpty()) {
            throw new IllegalArgumentException("decide concept matches command has no matches");
        }

        if (matches.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("concept match command matches must not contain null");
        }
        matches = List.copyOf(matches);

        Set<SelectedTerm> selectedTerms = new HashSet<>();
        for (CandidateConceptMatch match : matches) {
            if (!selectedTerms.add(match.selectedTerm())) {
                throw new IllegalArgumentException(
                    "decide concept matches command has duplicate selected terms"
                );
            }
        }
    }
}
