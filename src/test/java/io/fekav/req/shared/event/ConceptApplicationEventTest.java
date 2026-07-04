package io.fekav.req.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.DomainEvent;
import io.fekav.req.shared.model.ConceptMatchDecision;
import io.fekav.req.shared.model.ConceptMatchDecisionStatus;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

class ConceptApplicationEventTest {

    @Test
    void createConceptResolutionDecidedEvent_preservesDecisionWithMetadata() {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        ConceptMatchDecision decision = autoCreateNewDecision();

        // When
        ConceptResolutionDecidedEvent event =
            ConceptResolutionDecidedEvent.create(correlationId, decision);

        // Then
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.decision()).isEqualTo(decision);
    }

    @Test
    void containsNoMatch_whenConceptResolutionDecidedEventIsCreated() {
        // Act
        String[] componentNames = recordComponentNames(ConceptResolutionDecidedEvent.class);

        // Assert
        assertThat(componentNames).doesNotContain("match");
    }

    private String[] recordComponentNames(Class<? extends Record> recordType) {
        return java.util.Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toArray(String[]::new);
    }

    private ConceptMatchDecision autoCreateNewDecision() {
        return new ConceptMatchDecision(
            new RequirementElement(RequirementElementType.CONDITION, "after timeout"),
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        );
    }
}
