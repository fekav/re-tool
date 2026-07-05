package io.fekav.req.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.DomainEvent;
import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.NodeMatchReviewRequest;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

class NodeApplicationEventTest {

    @Test
    void createNodeResolutionDecidedEvent_preservesDecisionWithMetadata() {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        NodeMatchDecision decision = autoCreateNewDecision();

        // When
        NodeResolutionDecidedEvent event =
            NodeResolutionDecidedEvent.create(correlationId, decision);

        // Then
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isInstanceOf(NodeResolutionEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.decision()).isEqualTo(decision);
    }

    @Test
    void containsNoMatch_whenNodeResolutionDecidedEventIsCreated() {
        // Act
        String[] componentNames = recordComponentNames(NodeResolutionDecidedEvent.class);

        // Assert
        assertThat(componentNames).doesNotContain("match");
    }

    @Test
    void createNodeResolutionReviewRequiredEvent_preservesReviewRequestWithMetadata() {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        NodeMatchReviewRequest reviewRequest = reviewRequest();

        // When
        NodeResolutionReviewRequiredEvent event =
            NodeResolutionReviewRequiredEvent.create(correlationId, reviewRequest);

        // Then
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isInstanceOf(NodeResolutionEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.reviewRequest()).isEqualTo(reviewRequest);
    }

    @Test
    void containsNoDecision_whenNodeResolutionReviewRequiredEventIsCreated() {
        // Act
        String[] componentNames =
            recordComponentNames(NodeResolutionReviewRequiredEvent.class);

        // Assert
        assertThat(componentNames).doesNotContain("decision");
    }

    private String[] recordComponentNames(Class<? extends Record> recordType) {
        return java.util.Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toArray(String[]::new);
    }

    private NodeMatchDecision autoCreateNewDecision() {
        return new NodeMatchDecision(
            new RequirementElement(RequirementElementType.CONDITION, "after timeout"),
            NodeMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        );
    }

    private NodeMatchReviewRequest reviewRequest() {
        return new NodeMatchReviewRequest(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            List.of(candidate()),
            "Candidate requires review"
        );
    }

    private RetrievedCandidateNode candidate() {
        return new RetrievedCandidateNode(
            new CandidateNode("concept-1", "Billing Service", NodeType.CONCEPT),
            List.of(new RetrievalEvidence("nodeName", "matched node name", 0.8))
        );
    }
}
