package io.fekav.req.syntaxextraction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.shared.event.SyntaxExtractedEvent;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.RawText;
import io.fekav.req.syntaxextraction.domain.Action;
import io.fekav.req.syntaxextraction.domain.Subject;
import io.fekav.req.syntaxextraction.domain.TargetObject;

class ExtractEntitiesCommandHandlerTest {

    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final SyntaxExtraction syntaxExtraction =
        mock(SyntaxExtraction.class);
    private final ExtractSyntaxCommandHandler handler = new ExtractSyntaxCommandHandler(
        eventPublisher,
        syntaxExtraction
    );

    @Test
    void returnsCompatibleResponseAndPublishesEvent_whenExtractionSucceeds() {
        String rawText = "The reporting dashboard shall export monthly usage metrics.";
        Action action = action();
        when(syntaxExtraction.extractSyntax(new RawText(rawText)))
            .thenReturn(action);

        ExtractSyntaxResponse result = handler.handle(new ExtractSyntaxCommand(rawText));

        assertThat(result.requirementElements().SUBJECT()).isEqualTo("reporting dashboard");
        assertThat(result.requirementElements().ACTION()).isEqualTo("shall export");
        assertThat(result.requirementElements().OBJECT()).isEqualTo("monthly usage metrics");
        assertThat(result.requirementElements().CONSTRAINT()).isEmpty();
        assertThat(result.requirementElements().CONDITION()).isEmpty();
        verify(syntaxExtraction).extractSyntax(new RawText(rawText));

        ArgumentCaptor<List<DomainEvent>> domainEvents = eventCaptor();
        verify(eventPublisher).publishAll(domainEvents.capture());
        assertThat(domainEvents.getValue())
            .singleElement()
            .satisfies(domainEvent -> {
                assertThat(domainEvent).isInstanceOf(SyntaxExtractedEvent.class);
                SyntaxExtractedEvent event = (SyntaxExtractedEvent) domainEvent;
                assertThat(event.action()).isEqualTo(action);
            });
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<DomainEvent>> eventCaptor() {
        return ArgumentCaptor.forClass(List.class);
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
