package io.fekav.req.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.SourceMetadata;

class IngestRequirementCommandHandlerTest {

    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final IngestRequirementCommandHandler handler =
        new IngestRequirementCommandHandler(eventPublisher);

    @Test
    void returnsRequirementIngestedEventAndPublishesSameEvent_whenRequirementIsIngested() {
        // Arrange
        String originalText = "  The checkout service must support guest checkout.  ";
        IngestRequirementCommand command = new IngestRequirementCommand(originalText);

        // Act
        RequirementIngestedEvent result = handler.handle(command);

        // Assert
        assertThat(result).isInstanceOf(ApplicationEvent.class);
        assertThat(result.correlationId()).isNotNull();
        assertThat(result.provenance().getOriginalText().text()).isEqualTo(originalText);
        assertThat(result.provenance().getSourceMetadata()).isEqualTo(SourceMetadata.apiRequest());
        assertThat(result.provenance().getIngestedAt()).isEqualTo(result.occurredAt());
        verify(eventPublisher).publish(result);
    }

    @Test
    void rejectsIngestRequirementCommand_whenOriginalTextIsBlank() {
        // Act / Assert
        assertThatThrownBy(() -> new IngestRequirementCommand(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ingestion command has no original text");
    }
}
