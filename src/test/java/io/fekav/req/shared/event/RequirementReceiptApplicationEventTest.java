package io.fekav.req.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
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
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.OriginalText;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.RawText;
import io.fekav.req.shared.model.SourceMetadata;

class RequirementReceiptApplicationEventTest {

    @Test
    void isApplicationEvent_whenRequirementIsIngested() {
        // Arrange
        Provenance provenance = provenance();

        // Act
        RequirementIngestedEvent event = RequirementIngestedEvent.create(provenance);

        // Assert
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.correlationId()).isNotNull();
    }

    @Test
    void containsOnlyReceiptMetadata_whenRequirementIsIngested() {
        // Act
        String[] componentNames = recordComponentNames(RequirementIngestedEvent.class);

        // Assert
        assertThat(componentNames)
            .containsExactly("eventId", "occurredAt", "correlationId", "provenance")
            .doesNotContain("requirementId", "rawText");
    }

    @Test
    void isApplicationEvent_whenRequirementIsClassified() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();

        // Act
        RequirementClassifiedEvent event = RequirementClassifiedEvent.create(
            correlationId,
            new RawText("The checkout service must support guest checkout."),
            classification()
        );

        // Assert
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.correlationId()).isEqualTo(correlationId);
    }

    @Test
    void isApplicationEvent_whenRequirementElementsAreExtracted() {
        // Arrange
        CorrelationId correlationId = CorrelationId.create();

        // Act
        RequirementElementsExtractedEvent event = RequirementElementsExtractedEvent.create(
            correlationId,
            new RawText("The dashboard shall export metrics."),
            action()
        );

        // Assert
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.correlationId()).isEqualTo(correlationId);
    }

    private String[] recordComponentNames(Class<? extends Record> recordType) {
        return java.util.Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toArray(String[]::new);
    }

    private Provenance provenance() {
        return Provenance.create(
            ElementId.create(),
            new OriginalText("  The checkout service must support guest checkout.  "),
            SourceMetadata.apiRequest(),
            Instant.parse("2026-07-02T10:15:30Z")
        );
    }

    private Classification classification() {
        return new Classification(
            RequirementType.REQUIREMENT,
            RequirementProperty.FUNCTIONAL,
            new ConfidenceScore(0.94),
            new Rationale("The text assigns a verifiable obligation to the service.")
        );
    }

    private Action action() {
        return new Action(
            ElementId.create(),
            "shall export",
            new Subject(ElementId.create(), "reporting dashboard"),
            new TargetObject(ElementId.create(), "monthly usage metrics"),
            Set.of(),
            Set.of()
        );
    }
}
