package io.fekav.req.conceptmatching.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.fekav.req.shared.model.SelectedTerm;

public record ConceptMatchDecisionSet(
    List<ConceptMatchDecision> decisions
) {

    public ConceptMatchDecisionSet {
        if (decisions == null || decisions.isEmpty()) {
            throw new IllegalArgumentException("concept match decision set must not be empty");
        }

        if (decisions.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("concept match decision set must not contain null");
        }
        decisions = List.copyOf(decisions);

        Set<SelectedTerm> selectedTerms = new HashSet<>();
        for (ConceptMatchDecision decision : decisions) {
            if (!selectedTerms.add(decision.selectedTerm())) {
                throw new IllegalArgumentException(
                    "concept match decision set must contain one decision per selected term"
                );
            }
        }
    }
}
