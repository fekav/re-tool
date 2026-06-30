package io.fekav.req.shared.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record CandidateConceptMatchSet(
    List<CandidateConceptMatch> matches
) {

    public CandidateConceptMatchSet {
        if (matches == null || matches.isEmpty()) {
            throw new IllegalArgumentException("candidate concept match set must not be empty");
        }

        matches = List.copyOf(matches);
        if (matches.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("candidate concept match set must not contain null");
        }

        Set<SelectedTerm> selectedTerms = new HashSet<>();
        for (CandidateConceptMatch match : matches) {
            if (!selectedTerms.add(match.selectedTerm())) {
                throw new IllegalArgumentException(
                    "candidate concept match set must contain one match per selected term"
                );
            }
        }
    }
}
