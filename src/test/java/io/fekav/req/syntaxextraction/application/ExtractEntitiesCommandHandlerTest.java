package io.fekav.req.syntaxextraction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.shared.event.EntitiesExtractedEvent;
import io.fekav.req.shared.model.RawText;
import io.fekav.req.syntaxextraction.domain.RequirementSyntax;
import io.fekav.req.syntaxextraction.domain.RequirementSyntaxType;

class ExtractEntitiesCommandHandlerTest {

    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final RequirementSyntaxExtraction requirementSyntaxExtraction =
        mock(RequirementSyntaxExtraction.class);
    private final ExtractSyntaxCommandHandler handler = new ExtractSyntaxCommandHandler(
        eventPublisher,
        requirementSyntaxExtraction
    );

    @Test
    void returnsRequirementSyntaxAndPublishesEvent_whenExtractionSucceeds() {
        String rawText = "The reporting dashboard shall export monthly usage metrics.";
        RequirementSyntax requirementSyntax = new RequirementSyntax(Map.of(
            RequirementSyntaxType.SUBJECT, "reporting dashboard",
            RequirementSyntaxType.ACTION, "shall export",
            RequirementSyntaxType.OBJECT, "monthly usage metrics"
        ));
        when(requirementSyntaxExtraction.extractRequirementSyntax(new RawText(rawText)))
            .thenReturn(requirementSyntax);

        RequirementSyntax result = handler.handle(new ExtractSyntaxCommand(rawText));

        assertThat(result).isEqualTo(requirementSyntax);
        verify(requirementSyntaxExtraction).extractRequirementSyntax(new RawText(rawText));

        ArgumentCaptor<List<DomainEvent>> domainEvents = eventCaptor();
        verify(eventPublisher).publishAll(domainEvents.capture());
        assertThat(domainEvents.getValue())
            .singleElement()
            .isInstanceOf(EntitiesExtractedEvent.class);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<DomainEvent>> eventCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
