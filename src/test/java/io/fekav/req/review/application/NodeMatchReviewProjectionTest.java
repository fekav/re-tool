package io.fekav.req.review.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.review.domain.NodeMatchReviewId;
import io.fekav.req.review.domain.PendingNodeMatchReview;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.event.NodeResolutionReviewRequiredEvent;
import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.NodeMatchReviewRequest;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

class NodeMatchReviewProjectionTest {

    private static final CorrelationId CORRELATION_ID =
        new CorrelationId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final Instant REQUESTED_AT =
        Instant.parse("2026-07-07T10:15:30Z");
    private static final RequirementElement SUBJECT =
        new RequirementElement(RequirementElementType.SUBJECT, "checkout service");

    private final NodeMatchReviewProjection projection = new NodeMatchReviewProjection();

    @Test
    void listsPendingReview_whenReviewRequiredEventIsOpened() {
        // Given
        NodeResolutionReviewRequiredEvent event = reviewRequiredEvent(
            SUBJECT,
            REQUESTED_AT
        );

        // When
        projection.apply(event);

        // Then
        assertThat(projection.pendingReviews())
            .singleElement()
            .satisfies(review -> {
                assertThat(review.reviewId())
                    .isEqualTo(NodeMatchReviewId.from(CORRELATION_ID, SUBJECT).value());
                assertThat(review.correlationId()).isEqualTo(CORRELATION_ID);
                assertThat(review.requirementElement()).isEqualTo(SUBJECT);
                assertThat(review.candidates()).containsExactly(candidate("checkout-service"));
                assertThat(review.rationale()).isEqualTo("Candidate needs review before mapping");
                assertThat(review.requestedAt()).isEqualTo(REQUESTED_AT);
            });
    }

    @Test
    void keepsOnePendingReview_whenEquivalentReviewRequiredEventIsOpenedTwice() {
        // Given
        NodeResolutionReviewRequiredEvent firstEvent = reviewRequiredEvent(
            SUBJECT,
            REQUESTED_AT
        );
        NodeResolutionReviewRequiredEvent redeliveredEvent = reviewRequiredEvent(
            SUBJECT,
            REQUESTED_AT.plusSeconds(5)
        );

        // When
        projection.apply(firstEvent);
        projection.apply(redeliveredEvent);

        // Then
        assertThat(projection.pendingReviews())
            .singleElement()
            .satisfies(review -> assertThat(review.requestedAt()).isEqualTo(REQUESTED_AT));
    }

    @Test
    void listsPendingReviewsInDeterministicOrder() {
        // Given
        RequirementElement action =
            new RequirementElement(RequirementElementType.ACTION, "pay");
        RequirementElement object =
            new RequirementElement(RequirementElementType.OBJECT, "invoice");

        // When
        projection.apply(reviewRequiredEvent(object, REQUESTED_AT.plusSeconds(2)));
        projection.apply(reviewRequiredEvent(SUBJECT, REQUESTED_AT));
        projection.apply(reviewRequiredEvent(action, REQUESTED_AT.plusSeconds(1)));

        // Then
        assertThat(projection.pendingReviews())
            .extracting(PendingNodeMatchReview::requirementElement)
            .containsExactly(SUBJECT, action, object);
    }

    @Test
    void closesPendingReview_whenMatchingDecisionEventIsClosed() {
        // Given
        projection.apply(reviewRequiredEvent(SUBJECT, REQUESTED_AT));
        NodeResolutionDecidedEvent decisionEvent = NodeResolutionDecidedEvent.create(
            CORRELATION_ID,
            reviewMapDecision(SUBJECT, "checkout-service")
        );

        // When
        projection.apply(decisionEvent);

        // Then
        assertThat(projection.pendingReviews()).isEmpty();
        assertThat(projection.pendingReview(NodeMatchReviewId.from(CORRELATION_ID, SUBJECT).value()))
            .isEmpty();
    }

    @Test
    void keepsPendingReviewOpen_whenDecisionDoesNotMatchReview() {
        // Given
        projection.apply(reviewRequiredEvent(SUBJECT, REQUESTED_AT));
        NodeResolutionDecidedEvent decisionEvent = NodeResolutionDecidedEvent.create(
            CORRELATION_ID,
            reviewMapDecision(
                new RequirementElement(RequirementElementType.ACTION, "pay"),
                "checkout-service"
            )
        );

        // When
        projection.apply(decisionEvent);

        // Then
        assertThat(projection.pendingReviews()).hasSize(1);
        assertThat(projection.pendingReview(NodeMatchReviewId.from(CORRELATION_ID, SUBJECT).value()))
            .hasValueSatisfying(review ->
                assertThat(review.requirementElement()).isEqualTo(SUBJECT)
            );
    }

    private NodeResolutionReviewRequiredEvent reviewRequiredEvent(
        RequirementElement element,
        Instant occurredAt
    ) {
        return new NodeResolutionReviewRequiredEvent(
            EventId.create(),
            occurredAt,
            CORRELATION_ID,
            new NodeMatchReviewRequest(
                element,
                List.of(candidate("checkout-service")),
                "Candidate needs review before mapping"
            )
        );
    }

    private NodeMatchDecision reviewMapDecision(
        RequirementElement element,
        String candidateKey
    ) {
        return new NodeMatchDecision(
            element,
            NodeMatchDecisionStatus.REVIEW_MAP_EXISTING,
            List.of(candidate(candidateKey)),
            "Domain reviewer selected this candidate."
        );
    }

    private RetrievedCandidateNode candidate(String candidateKey) {
        return new RetrievedCandidateNode(
            new CandidateNode(candidateKey, "Checkout Service", NodeType.CONCEPT),
            List.of(new RetrievalEvidence("nodeName", "matched node name", 0.82))
        );
    }
}
