package io.fekav.req.review.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;

public record PendingNodeMatchReview(
    String reviewId,
    CorrelationId correlationId,
    RequirementElement requirementElement,
    List<RetrievedCandidateNode> candidates,
    String rationale,
    Instant requestedAt
) {

    public PendingNodeMatchReview {
        if (reviewId == null || reviewId.isBlank()) {
            throw new IllegalArgumentException("node match review id must not be blank");
        }
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(requirementElement, "requirementElement must not be null");
        Objects.requireNonNull(candidates, "review candidates must not be null");
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("review rationale must not be blank");
        }
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        if (candidates.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("review candidates must not contain null");
        }

        reviewId = reviewId.strip();
        candidates = List.copyOf(candidates);
        rationale = rationale.strip();
    }
}
