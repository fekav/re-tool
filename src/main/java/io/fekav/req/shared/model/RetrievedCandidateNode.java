package io.fekav.req.shared.model;

import java.util.List;
import java.util.Objects;

public record RetrievedCandidateNode(
    CandidateNode candidate,
    List<RetrievalEvidence> evidence
) {

    public RetrievedCandidateNode {
        Objects.requireNonNull(candidate, "candidate must not be null");
        if (evidence == null || evidence.isEmpty()) {
            throw new IllegalArgumentException("retrieved candidate evidence must not be empty");
        }
        evidence = List.copyOf(evidence);
        if (evidence.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("retrieved candidate evidence must not contain null");
        }
    }
}
