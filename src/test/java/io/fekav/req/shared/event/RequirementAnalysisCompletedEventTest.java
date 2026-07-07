package io.fekav.req.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.DomainEvent;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.Rationale;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.classification.domain.RequirementType;
import io.fekav.req.extraction.domain.Action;
import io.fekav.req.extraction.domain.Subject;
import io.fekav.req.extraction.domain.TargetObject;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.OriginalText;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.SourceMetadata;

class RequirementAnalysisCompletedEventTest {

    @Test
    void preservesAnalysisResultAndSetsMetadata_whenRequirementAnalysisCompletes() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        Provenance provenance = provenance();
        Classification classification = classification();
        Action action = action();
        List<NodeMatchDecision> nodeMatchDecisions = List.of(nodeMatchDecision());

        // Act
        RequirementAnalysisCompletedEvent event = RequirementAnalysisCompletedEvent.create(
            correlationId,
            provenance,
            classification,
            action,
            nodeMatchDecisions
        );

        // Assert
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.provenance()).isEqualTo(provenance);
        assertThat(event.classification()).isEqualTo(classification);
        assertThat(event.action()).isEqualTo(action);
        assertThat(event.nodeMatchDecisions())
            .containsExactlyElementsOf(nodeMatchDecisions);
    }

    @Test
    void containsNoRequirementId_whenRequirementAnalysisCompletes() {
        // Act
        String[] componentNames = recordComponentNames(RequirementAnalysisCompletedEvent.class);

        // Assert
        assertThat(componentNames).doesNotContain("requirementId");
    }

    @Test
    void containsNoNodeMatchResults_whenRequirementAnalysisCompletes() {
        // Act
        String[] componentNames = recordComponentNames(RequirementAnalysisCompletedEvent.class);

        // Assert
        assertThat(componentNames).doesNotContain("nodeMatchResults");
    }

    @Test
    void containsNoMatch_whenRequirementAnalysisCompletes() {
        // Act
        String[] componentNames = recordComponentNames(RequirementAnalysisCompletedEvent.class);

        // Assert
        assertThat(componentNames).doesNotContain("match");
    }

    private String[] recordComponentNames(Class<? extends Record> recordType) {
        return java.util.Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toArray(String[]::new);
    }

    private Provenance provenance() {
        return Provenance.create(
            ElementId.create(),
            new OriginalText("The login form must validate credentials."),
            SourceMetadata.apiRequest(),
            Instant.parse("2026-07-02T12:00:00Z")
        );
    }

    private Classification classification() {
        return new Classification(
            RequirementType.REQUIREMENT,
            RequirementProperty.FUNCTIONAL,
            new ConfidenceScore(0.94),
            new Rationale("The sentence expresses a verifiable obligation.")
        );
    }

    private Action action() {
        return new Action(
            ElementId.create(),
            "must validate",
            new Subject(ElementId.create(), "login form"),
            new TargetObject(ElementId.create(), "credentials"),
            Set.of(),
            Set.of()
        );
    }

    private NodeMatchDecision nodeMatchDecision() {
        RequirementElement element =
            new RequirementElement(RequirementElementType.SUBJECT, "login form");
        return new NodeMatchDecision(
            element,
            NodeMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        );
    }
}
