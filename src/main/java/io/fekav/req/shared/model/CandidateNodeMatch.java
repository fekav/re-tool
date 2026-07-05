package io.fekav.req.shared.model;

import java.util.List;
import java.util.Objects;

public record CandidateNodeMatch(
    RequirementElement requirementElement,
    List<RetrievedCandidateNode> candidates
) {

    public CandidateNodeMatch {
        Objects.requireNonNull(requirementElement, "selectedTerm must not be null");
        Objects.requireNonNull(candidates, "candidates must not be null");

        if (candidates.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("candidates must not contain null");
        }
        candidates = List.copyOf(candidates);
    }
}
