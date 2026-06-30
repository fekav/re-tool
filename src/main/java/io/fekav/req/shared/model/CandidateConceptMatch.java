package io.fekav.req.shared.model;

import java.util.List;
import java.util.Objects;

public record CandidateConceptMatch(
    SelectedTerm selectedTerm,
    List<RetrievedCandidateConcept> candidates
) {

    public CandidateConceptMatch {
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");
        Objects.requireNonNull(candidates, "candidates must not be null");

        candidates = List.copyOf(candidates);
        if (candidates.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("candidates must not contain null");
        }
    }
}
