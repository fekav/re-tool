package io.fekav.req.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.event.NodeResolutionReviewRequiredEvent;
import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.NodeMatchReviewRequest;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

class IngestionOutcomeStoreTest {

    private final IngestionOutcomeStore store = new IngestionOutcomeStore();

    @Test
    void recordsReviewRequiredOutcome_whenNodeResolutionRequiresReview() {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        NodeResolutionReviewRequiredEvent event = new NodeResolutionReviewRequiredEvent(
            EventId.create(),
            Instant.now(),
            correlationId,
            new NodeMatchReviewRequest(
                new RequirementElement(RequirementElementType.SUBJECT, "test"),
                List.of(new RetrievedCandidateNode(
                    new CandidateNode("key", "name", NodeType.CONCEPT),
                    List.of(new RetrievalEvidence("field", "match", 1.0))
                )),
                "rationale"
            )
        );

        // When
        store.onNodeResolutionReviewRequired(event);

        // Then
        assertThat(store.consume(correlationId))
            .hasValueSatisfying(result -> {
                assertThat(result.correlationId()).isEqualTo(correlationId);
                assertThat(result.status()).isEqualTo(IngestRequirementResult.Status.REVIEW_REQUIRED);
                assertThat(result.message()).isEqualTo("Requirement requires review.");
            });
    }
}
