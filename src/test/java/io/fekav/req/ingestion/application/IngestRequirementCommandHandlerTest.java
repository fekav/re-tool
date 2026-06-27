package io.fekav.req.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.ingestion.domain.RequirementIngestedEvent;
import io.fekav.req.ingestion.domain.RequirementIngestion;

class IngestRequirementCommandHandlerTest {

    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final RequirementTextNormalizationPolicy normalizationPolicy =
        mock(RequirementTextNormalizationPolicy.class);
    private final IngestRequirementCommandHandler handler = new IngestRequirementCommandHandler(
        eventPublisher,
        normalizationPolicy
    );

    @Test
    void returnsRequirementIngestionAndPublishesEvent_whenIngestionSucceeds() {
        String rawText = "  The system shall export reports.  ";
        String normalizedText = "The system shall export reports.";
        when(normalizationPolicy.normalize(rawText)).thenReturn(normalizedText);

        RequirementIngestion result = handler.handle(
            new IngestRequirementCommand("API_REQUEST", "req-123", rawText)
        );

        assertThat(result.provenance().sourceType()).isEqualTo("API_REQUEST");
        assertThat(result.provenance().sourceId()).isEqualTo("req-123");
        assertThat(result.originalText()).isEqualTo(rawText);
        assertThat(result.normalizedText()).isEqualTo(normalizedText);
        verify(normalizationPolicy).normalize(rawText);

        ArgumentCaptor<List<DomainEvent>> domainEvents = eventCaptor();
        verify(eventPublisher).publishAll(domainEvents.capture());
        assertThat(domainEvents.getValue())
            .singleElement()
            .satisfies(domainEvent -> {
                assertThat(domainEvent).isInstanceOf(RequirementIngestedEvent.class);
                RequirementIngestedEvent event = (RequirementIngestedEvent) domainEvent;
                assertThat(event.ingestionId()).isEqualTo(result.id());
                assertThat(event.originalText()).isEqualTo(rawText);
                assertThat(event.normalizedText()).isEqualTo(normalizedText);
            });
    }

    @Test
    void rejectsIngestRequirementCommand_whenSourceTypeIsBlank() {
        assertThatThrownBy(() -> new IngestRequirementCommand(" ", "req-123", "raw"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ingestion command has no source type");
    }

    @Test
    void rejectsIngestRequirementCommand_whenSourceIdIsBlank() {
        assertThatThrownBy(() -> new IngestRequirementCommand("API_REQUEST", " ", "raw"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ingestion command has no source id");
    }

    @Test
    void rejectsIngestRequirementCommand_whenRawTextIsBlank() {
        assertThatThrownBy(() -> new IngestRequirementCommand("API_REQUEST", "req-123", " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ingestion command has no raw text");
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<DomainEvent>> eventCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
