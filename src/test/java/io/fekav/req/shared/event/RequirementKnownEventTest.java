package io.fekav.req.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.DomainEvent;
import io.fekav.req.shared.model.GraphNodeReference;
import io.fekav.req.shared.model.NodeType;

class RequirementKnownEventTest {

    @Test
    void preservesKnownAssertionWithMetadata_whenRequirementIsAlreadyKnown() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();
        GraphNodeReference subject =
            new GraphNodeReference(NodeType.CONCEPT, "login form", "Login Form");
        GraphNodeReference predicate =
            new GraphNodeReference(NodeType.PREDICATE, "validate", "Validate");
        GraphNodeReference object =
            new GraphNodeReference(NodeType.CONCEPT, "credentials", "Credentials");

        // Act
        RequirementKnownEvent event = RequirementKnownEvent.create(
            correlationId,
            subject,
            predicate,
            object
        );

        // Assert
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.subject()).isEqualTo(subject);
        assertThat(event.predicate()).isEqualTo(predicate);
        assertThat(event.object()).isEqualTo(object);
    }

    @Test
    void carriesOnlyMetadataAndAssertionTriple_whenRequirementIsAlreadyKnown() {
        // Act
        String[] componentNames = recordComponentNames(RequirementKnownEvent.class);

        // Assert
        assertThat(componentNames)
            .containsExactly(
                "eventId",
                "occurredAt",
                "correlationId",
                "subject",
                "predicate",
                "object"
            );
    }

    private String[] recordComponentNames(Class<? extends Record> recordType) {
        return java.util.Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toArray(String[]::new);
    }
}
