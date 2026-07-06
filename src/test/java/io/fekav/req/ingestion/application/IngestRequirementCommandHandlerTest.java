package io.fekav.req.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.SourceMetadata;

@ExtendWith(MockitoExtension.class)
class IngestRequirementCommandHandlerTest {

    @Mock
    EventPublisher eventPublisher;

    IngestionOutcomeStore outcomeStore;
    IngestRequirementCommandHandler handler;

    @BeforeEach
    void setUp() {
        outcomeStore = new IngestionOutcomeStore();
        handler = new IngestRequirementCommandHandler(eventPublisher, outcomeStore);
    }

    @Test
    void returnsRecordedResultAndPublishesReceipt_whenRequirementIsRecorded() {
        // Arrange
        String originalText = "  The checkout service must support guest checkout.  ";
        IngestRequirementCommand command = new IngestRequirementCommand(originalText);
        doAnswer(invocation -> {
            RequirementIngestedEvent event = (RequirementIngestedEvent) invocation
                .getArgument(0);
            outcomeStore.record(IngestRequirementResult.recorded(event.correlationId()));
            return null;
        }).when(eventPublisher).publish(any(ApplicationEvent.class));

        // Act
        IngestRequirementResult result = handler.handle(command);

        // Assert
        ArgumentCaptor<ApplicationEvent> eventCaptor =
            ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
            .isInstanceOfSatisfying(RequirementIngestedEvent.class, event -> {
                assertThat(result.correlationId()).isEqualTo(event.correlationId());
                assertThat(event.provenance().getOriginalText().text()).isEqualTo(originalText);
                assertThat(event.provenance().getSourceMetadata())
                    .isEqualTo(SourceMetadata.apiRequest());
                assertThat(event.provenance().getIngestedAt()).isEqualTo(event.occurredAt());
            });
        assertThat(result.status()).isEqualTo(IngestRequirementResult.Status.RECORDED);
        assertThat(result.message()).isEqualTo("Requirement recorded.");
    }

    @Test
    void returnsInternalError_whenNoFinalOutcomeIsRecorded() {
        // Arrange
        IngestRequirementCommand command = new IngestRequirementCommand(
            "The checkout service must support guest checkout."
        );

        // Act
        IngestRequirementResult result = handler.handle(command);

        // Assert
        verify(eventPublisher).publish(any(ApplicationEvent.class));
        assertThat(result.status()).isEqualTo(IngestRequirementResult.Status.INTERNAL_ERROR);
        assertThat(result.message()).isEqualTo("No final ingestion outcome was recorded.");
    }

    @Test
    void returnsInternalError_whenReceiptPublishingFails() {
        // Arrange
        IngestRequirementCommand command = new IngestRequirementCommand(
            "The checkout service must support guest checkout."
        );
        doThrow(new RuntimeException("downstream failure"))
            .when(eventPublisher)
            .publish(any(ApplicationEvent.class));

        // Act
        IngestRequirementResult result = handler.handle(command);

        // Assert
        assertThat(result.status()).isEqualTo(IngestRequirementResult.Status.INTERNAL_ERROR);
        assertThat(result.message()).isEqualTo("Requirement ingestion failed internally.");
    }

    @Test
    void rejectsIngestRequirementCommand_whenOriginalTextIsBlank() {
        // Act / Assert
        assertThatThrownBy(() -> new IngestRequirementCommand(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ingestion command has no original text");
    }
}
