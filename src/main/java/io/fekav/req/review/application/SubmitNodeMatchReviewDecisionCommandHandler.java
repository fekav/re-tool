package io.fekav.req.review.application;

import java.util.List;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.review.domain.PendingNodeMatchReview;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SubmitNodeMatchReviewDecisionCommandHandler
    implements CommandHandler<
        NodeResolutionDecidedEvent,
        SubmitNodeMatchReviewDecisionCommand
    > {

    private final NodeMatchReviewProjection reviewProjection;
    private final EventPublisher eventPublisher;

    @Inject
    public SubmitNodeMatchReviewDecisionCommandHandler(
        NodeMatchReviewProjection reviewProjection,
        EventPublisher eventPublisher
    ) {
        this.reviewProjection = reviewProjection;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public NodeResolutionDecidedEvent handle(
        SubmitNodeMatchReviewDecisionCommand command
    ) {
        PendingNodeMatchReview review = pendingReviewForDecision(command.reviewId());
        NodeMatchDecision decision = toNodeMatchDecision(review, command);
        NodeResolutionDecidedEvent event = NodeResolutionDecidedEvent.create(
            review.correlationId(),
            decision
        );

        eventPublisher.publish(event);
        reviewProjection.apply(event);

        return event;
    }

    @Override
    public Class<SubmitNodeMatchReviewDecisionCommand> commandType() {
        return SubmitNodeMatchReviewDecisionCommand.class;
    }

    private PendingNodeMatchReview pendingReviewForDecision(String reviewId) {
        return reviewProjection
            .pendingReview(reviewId)
            .orElseThrow(() -> new IllegalArgumentException(
                unavailableReviewMessage(reviewId)
            ));
    }

    private String unavailableReviewMessage(String reviewId) {
        if (reviewProjection.containsReview(reviewId)) {
            return "pending node match review is already closed: " + reviewId;
        }
        return "pending node match review not found: " + reviewId;
    }

    private NodeMatchDecision toNodeMatchDecision(
        PendingNodeMatchReview review,
        SubmitNodeMatchReviewDecisionCommand command
    ) {
        return switch (command.decision()) {
            case "MAP_EXISTING" -> mapExistingDecision(
                review,
                command.candidateKey(),
                command.rationale()
            );
            case "CREATE_NEW" -> createNewDecision(
                review,
                command.candidateKey(),
                command.rationale()
            );
            default -> throw new IllegalArgumentException(
                "unsupported review decision: " + command.decision()
            );
        };
    }

    private NodeMatchDecision mapExistingDecision(
        PendingNodeMatchReview review,
        String candidateKey,
        String rationale
    ) {
        if (candidateKey == null) {
            throw new IllegalArgumentException(
                "map-existing review decisions require a candidate key"
            );
        }

        RetrievedCandidateNode selectedCandidate = review
            .candidates()
            .stream()
            .filter(candidate ->
                candidate.candidate().candidateKey().equals(candidateKey)
            )
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "candidate key does not belong to pending review: " +
                        candidateKey
                )
            );

        return new NodeMatchDecision(
            review.requirementElement(),
            NodeMatchDecisionStatus.REVIEW_MAP_EXISTING,
            List.of(selectedCandidate),
            rationale
        );
    }

    private NodeMatchDecision createNewDecision(
        PendingNodeMatchReview review,
        String candidateKey,
        String rationale
    ) {
        if (candidateKey != null) {
            throw new IllegalArgumentException(
                "create-new review decisions must not include a candidate key"
            );
        }

        return new NodeMatchDecision(
            review.requirementElement(),
            NodeMatchDecisionStatus.REVIEW_CREATE_NEW,
            List.of(),
            rationale
        );
    }
}
