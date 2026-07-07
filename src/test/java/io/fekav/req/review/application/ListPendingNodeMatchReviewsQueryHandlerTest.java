package io.fekav.req.review.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.review.domain.NodeMatchReviewId;
import io.fekav.req.shared.event.NodeResolutionReviewRequiredEvent;
import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.NodeMatchReviewRequest;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

class ListPendingNodeMatchReviewsQueryHandlerTest {

    @Test
    void returnsAllOpenPendingReviews() {
        // Given
        NodeMatchReviewProjection projection = new NodeMatchReviewProjection();
        CorrelationId correlationId =
            new CorrelationId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        RequirementElement element =
            new RequirementElement(RequirementElementType.SUBJECT, "checkout service");
        NodeResolutionReviewRequiredEvent event = new NodeResolutionReviewRequiredEvent(
            EventId.create(),
            Instant.parse("2026-07-07T10:15:30Z"),
            correlationId,
            new NodeMatchReviewRequest(
                element,
                List.of(candidate()),
                "Candidate needs review before mapping"
            )
        );
        projection.apply(event);
        ListPendingNodeMatchReviewsQueryHandler handler =
            new ListPendingNodeMatchReviewsQueryHandler(projection);

        // When
        ListPendingNodeMatchReviewsQuery.Result result =
            handler.handle(new ListPendingNodeMatchReviewsQuery());

        // Then
        assertThat(result.reviews())
            .singleElement()
            .satisfies(review -> {
                assertThat(review.reviewId())
                    .isEqualTo(NodeMatchReviewId.from(correlationId, element).value());
                assertThat(review.correlationId()).isEqualTo(correlationId);
                assertThat(review.requirementElement()).isEqualTo(element);
                assertThat(review.candidates()).containsExactly(candidate());
                assertThat(review.rationale()).isEqualTo("Candidate needs review before mapping");
                assertThat(review.requestedAt())
                    .isEqualTo(Instant.parse("2026-07-07T10:15:30Z"));
            });
    }

    @Test
    void exposesHandledQueryType() {
        // Given
        ListPendingNodeMatchReviewsQueryHandler handler =
            new ListPendingNodeMatchReviewsQueryHandler(
                new NodeMatchReviewProjection()
            );

        // When
        Class<ListPendingNodeMatchReviewsQuery> queryType = handler.queryType();

        // Then
        assertThat(queryType).isEqualTo(ListPendingNodeMatchReviewsQuery.class);
    }

    private static RetrievedCandidateNode candidate() {
        return new RetrievedCandidateNode(
            new CandidateNode("checkout-service", "Checkout Service", NodeType.CONCEPT),
            List.of(new RetrievalEvidence("nodeName", "matched node name", 0.82))
        );
    }
}
