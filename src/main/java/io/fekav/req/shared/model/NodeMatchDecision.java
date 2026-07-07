package io.fekav.req.shared.model;

import java.util.List;
import java.util.Objects;

public record NodeMatchDecision(
    RequirementElement requirementElement,
    NodeMatchDecisionStatus status,
    List<RetrievedCandidateNode> candidates,
    String rationale
) {

    public NodeMatchDecision {
        Objects.requireNonNull(requirementElement, "requirementElement must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(candidates, "node match decision candidates must not be null");
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("node match decision rationale must not be blank");
        }

        if (candidates.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException(
                "node match decision candidates must not contain null"
            );
        }
        candidates = List.copyOf(candidates);

        rationale = rationale.strip();
        status.validatePayload(candidates);
    }
}
