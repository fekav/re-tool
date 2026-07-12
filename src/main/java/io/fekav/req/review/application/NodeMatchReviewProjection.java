package io.fekav.req.review.application;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.fekav.req.review.domain.NodeMatchReviewId;
import io.fekav.req.review.domain.PendingNodeMatchReview;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.event.NodeResolutionReviewRequiredEvent;
import io.fekav.req.shared.model.RequirementElement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class NodeMatchReviewProjection {

    private final ConcurrentMap<String, StoredNodeMatchReview> reviews =
        new ConcurrentHashMap<>();

    public void apply(@Observes NodeResolutionReviewRequiredEvent event) {
        PendingNodeMatchReview review = pendingReviewFrom(event);
        reviews.putIfAbsent(
            review.reviewId(),
            new StoredNodeMatchReview(review, true)
        );
    }

    public void apply(@Observes NodeResolutionDecidedEvent event) {
        RequirementElement decidedElement = event.decision().requirementElement();
        reviews.replaceAll((id, storedReview) ->
            storedReview.review().requirementElement().equals(decidedElement)
                ? storedReview.close()
                : storedReview
        );
    }

    public List<PendingNodeMatchReview> pendingReviews() {
        Map<RequirementElement, PendingNodeMatchReview> earliestOpenReviewsByElement =
            new LinkedHashMap<>();
        reviews
            .values()
            .stream()
            .filter(StoredNodeMatchReview::isOpen)
            .map(StoredNodeMatchReview::review)
            .sorted(
                Comparator
                    .comparing(PendingNodeMatchReview::requestedAt)
                    .thenComparing(PendingNodeMatchReview::reviewId)
            )
            .forEach(review -> earliestOpenReviewsByElement.putIfAbsent(
                review.requirementElement(),
                review
            ));
        return List.copyOf(earliestOpenReviewsByElement.values());
    }

    public Optional<PendingNodeMatchReview> pendingReview(String reviewId) {
        return Optional
            .ofNullable(reviews.get(reviewId))
            .filter(StoredNodeMatchReview::isOpen)
            .map(StoredNodeMatchReview::review);
    }

    public boolean containsReview(String reviewId) {
        return reviews.containsKey(reviewId);
    }

    private PendingNodeMatchReview pendingReviewFrom(
        NodeResolutionReviewRequiredEvent event
    ) {
        return new PendingNodeMatchReview(
            NodeMatchReviewId
                .from(event.correlationId(), event.reviewRequest().requirementElement())
                .value(),
            event.correlationId(),
            event.reviewRequest().requirementElement(),
            event.reviewRequest().candidates(),
            event.reviewRequest().rationale(),
            event.occurredAt()
        );
    }

    private record StoredNodeMatchReview(
        PendingNodeMatchReview review,
        boolean open
    ) {

        boolean isOpen() {
            return open;
        }

        StoredNodeMatchReview close() {
            return new StoredNodeMatchReview(review, false);
        }
    }
}
