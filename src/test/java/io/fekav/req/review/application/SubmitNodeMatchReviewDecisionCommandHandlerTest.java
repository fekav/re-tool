package io.fekav.req.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.review.domain.NodeMatchReviewId;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.event.NodeResolutionReviewRequiredEvent;
import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.NodeMatchReviewRequest;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

class SubmitNodeMatchReviewDecisionCommandHandlerTest {

    private static final CorrelationId CORRELATION_ID =
        new CorrelationId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final RequirementElement SUBJECT =
        new RequirementElement(RequirementElementType.SUBJECT, "checkout service");
    private static final Instant REQUESTED_AT =
        Instant.parse("2026-07-07T10:15:30Z");

    private final NodeMatchReviewProjection projection = new NodeMatchReviewProjection();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final SubmitNodeMatchReviewDecisionCommandHandler handler =
        new SubmitNodeMatchReviewDecisionCommandHandler(projection, eventPublisher);

    private String reviewId;

    @BeforeEach
    void openPendingReview() {
        projection.apply(new NodeResolutionReviewRequiredEvent(
            EventId.create(),
            REQUESTED_AT,
            CORRELATION_ID,
            new NodeMatchReviewRequest(
                SUBJECT,
                List.of(
                    candidate("checkout-service", "Checkout Service"),
                    candidate("payment-service", "Payment Service")
                ),
                "Candidate needs review before mapping"
            )
        ));
        reviewId = NodeMatchReviewId.from(CORRELATION_ID, SUBJECT).value();
    }

    @Test
    void publishesReviewMapDecisionAndClosesReview_whenExistingCandidateIsSelected() {
        // Given
        SubmitNodeMatchReviewDecisionCommand command =
            new SubmitNodeMatchReviewDecisionCommand(
                reviewId,
                "MAP_EXISTING",
                "checkout-service",
                "Domain reviewer selected this candidate."
            );

        // When
        NodeResolutionDecidedEvent result = handler.handle(command);

        // Then
        assertThat(result.correlationId()).isEqualTo(CORRELATION_ID);
        assertThat(result.decision().requirementElement()).isEqualTo(SUBJECT);
        assertThat(result.decision().status())
            .isEqualTo(NodeMatchDecisionStatus.REVIEW_MAP_EXISTING);
        assertThat(result.decision().candidates())
            .singleElement()
            .satisfies(candidate ->
                assertThat(candidate.candidate().candidateKey())
                    .isEqualTo("checkout-service")
            );
        assertThat(result.decision().rationale())
            .isEqualTo("Domain reviewer selected this candidate.");
        assertThat(eventPublisher.applicationEvents()).containsExactly(result);
        assertThat(projection.pendingReview(reviewId)).isEmpty();
    }

    @Test
    void publishesReviewCreateDecisionAndClosesReview_whenReviewerCreatesNewNode() {
        // Given
        SubmitNodeMatchReviewDecisionCommand command =
            new SubmitNodeMatchReviewDecisionCommand(
                reviewId,
                "CREATE_NEW",
                null,
                "Domain reviewer requested a new concept."
            );

        // When
        NodeResolutionDecidedEvent result = handler.handle(command);

        // Then
        assertThat(result.decision().status())
            .isEqualTo(NodeMatchDecisionStatus.REVIEW_CREATE_NEW);
        assertThat(result.decision().candidates()).isEmpty();
        assertThat(eventPublisher.applicationEvents()).containsExactly(result);
        assertThat(projection.pendingReview(reviewId)).isEmpty();
    }

    @Test
    void rejectsDecision_whenReviewIdDoesNotExist() {
        // Given
        SubmitNodeMatchReviewDecisionCommand command =
            new SubmitNodeMatchReviewDecisionCommand(
                "missing-review",
                "CREATE_NEW",
                null,
                "Domain reviewer requested a new concept."
            );

        // When / Then
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("pending node match review not found: missing-review");
        assertThat(eventPublisher.applicationEvents()).isEmpty();
    }

    @Test
    void rejectsDecision_whenCandidateDoesNotBelongToReview() {
        // Given
        SubmitNodeMatchReviewDecisionCommand command =
            new SubmitNodeMatchReviewDecisionCommand(
                reviewId,
                "MAP_EXISTING",
                "shipping-service",
                "Domain reviewer selected this candidate."
            );

        // When / Then
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate key does not belong to pending review: shipping-service");
        assertThat(eventPublisher.applicationEvents()).isEmpty();
        assertThat(projection.pendingReview(reviewId)).hasValueSatisfying(review ->
            assertThat(review.reviewId()).isEqualTo(reviewId)
        );
    }

    @Test
    void rejectsDecision_whenMapExistingCandidateKeyIsBlank() {
        // Given
        SubmitNodeMatchReviewDecisionCommand command =
            new SubmitNodeMatchReviewDecisionCommand(
                reviewId,
                "MAP_EXISTING",
                " ",
                "Domain reviewer selected this candidate."
            );

        // When / Then
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("map-existing review decisions require a candidate key");
    }

    @Test
    void rejectsDecision_whenCreateNewCarriesCandidateKey() {
        // Given
        SubmitNodeMatchReviewDecisionCommand command =
            new SubmitNodeMatchReviewDecisionCommand(
                reviewId,
                "CREATE_NEW",
                "checkout-service",
                "Domain reviewer requested a new concept."
            );

        // When / Then
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("create-new review decisions must not include a candidate key");
    }

    @Test
    void rejectsDecision_whenDecisionValueIsUnsupported() {
        // Given
        SubmitNodeMatchReviewDecisionCommand command =
            new SubmitNodeMatchReviewDecisionCommand(
                reviewId,
                "MERGE",
                "checkout-service",
                "Domain reviewer selected this candidate."
            );

        // When / Then
        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("unsupported review decision: MERGE");
        assertThat(projection.pendingReview(reviewId)).hasValueSatisfying(review ->
            assertThat(review.reviewId()).isEqualTo(reviewId)
        );
    }

    @Test
    void rejectsDecision_whenReviewWasAlreadyClosed() {
        // Given
        handler.handle(new SubmitNodeMatchReviewDecisionCommand(
            reviewId,
            "CREATE_NEW",
            null,
            "Domain reviewer requested a new concept."
        ));
        SubmitNodeMatchReviewDecisionCommand duplicateCommand =
            new SubmitNodeMatchReviewDecisionCommand(
                reviewId,
                "CREATE_NEW",
                null,
                "Domain reviewer requested a new concept."
            );

        // When / Then
        assertThatThrownBy(() -> handler.handle(duplicateCommand))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("pending node match review is already closed: " + reviewId);
    }

    @Test
    void keepsReviewOpen_whenPublishingDecisionFails() {
        // Given
        FailingEventPublisher failingPublisher = new FailingEventPublisher();
        SubmitNodeMatchReviewDecisionCommandHandler failingHandler =
            new SubmitNodeMatchReviewDecisionCommandHandler(
                projection,
                failingPublisher
            );
        SubmitNodeMatchReviewDecisionCommand command =
            new SubmitNodeMatchReviewDecisionCommand(
                reviewId,
                "CREATE_NEW",
                null,
                "Domain reviewer requested a new concept."
            );

        // When / Then
        assertThatThrownBy(() -> failingHandler.handle(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("event bus unavailable");
        assertThat(projection.pendingReview(reviewId)).hasValueSatisfying(review ->
            assertThat(review.reviewId()).isEqualTo(reviewId)
        );
    }

    private RetrievedCandidateNode candidate(String candidateKey, String label) {
        return new RetrievedCandidateNode(
            new CandidateNode(candidateKey, label, NodeType.CONCEPT),
            List.of(new RetrievalEvidence("nodeName", "matched node name", 0.82))
        );
    }

    private static final class RecordingEventPublisher implements EventPublisher {

        private final List<ApplicationEvent> applicationEvents = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
        }

        @Override
        public void publishAll(List<DomainEvent> events) {
        }

        @Override
        public void publish(ApplicationEvent event) {
            applicationEvents.add(event);
        }

        @Override
        public void publishApplicationEvents(List<ApplicationEvent> events) {
            applicationEvents.addAll(events);
        }

        List<ApplicationEvent> applicationEvents() {
            return applicationEvents;
        }
    }

    private static final class FailingEventPublisher implements EventPublisher {

        @Override
        public void publish(DomainEvent event) {
        }

        @Override
        public void publishAll(List<DomainEvent> events) {
        }

        @Override
        public void publish(ApplicationEvent event) {
            throw new IllegalStateException("event bus unavailable");
        }

        @Override
        public void publishApplicationEvents(List<ApplicationEvent> events) {
        }
    }
}
