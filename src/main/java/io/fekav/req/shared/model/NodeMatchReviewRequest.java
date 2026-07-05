package io.fekav.req.shared.model;

import java.util.List;
import java.util.Objects;

public record NodeMatchReviewRequest(
    RequirementElement requirementElement,
    List<RetrievedCandidateNode> candidates,
    String rationale
) {

    public NodeMatchReviewRequest {
        Objects.requireNonNull(requirementElement, "requirementElement must not be null");
        Objects.requireNonNull(candidates, "node match review candidates must not be null");
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("node match review rationale must not be blank");
        }

        if (candidates.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException(
                "node match review candidates must not contain null"
            );
        }
        candidates = List.copyOf(candidates);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                "node match review requests must contain at least one candidate"
            );
        }

        rationale = rationale.strip();
    }
}
