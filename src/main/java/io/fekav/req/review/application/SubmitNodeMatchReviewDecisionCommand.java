package io.fekav.req.review.application;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;

public record SubmitNodeMatchReviewDecisionCommand(
    String reviewId,
    String decision,
    String candidateKey,
    String rationale
) implements Command<NodeResolutionDecidedEvent> {

    public SubmitNodeMatchReviewDecisionCommand {
        if (reviewId == null || reviewId.isBlank()) {
            throw new IllegalArgumentException("node match review id must not be blank");
        }
        if (decision == null || decision.isBlank()) {
            throw new IllegalArgumentException("review decision must not be blank");
        }
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("review decision rationale must not be blank");
        }

        reviewId = reviewId.strip();
        decision = decision.strip();
        candidateKey = candidateKey == null || candidateKey.isBlank()
            ? null
            : candidateKey.strip();
        rationale = rationale.strip();
    }
}
