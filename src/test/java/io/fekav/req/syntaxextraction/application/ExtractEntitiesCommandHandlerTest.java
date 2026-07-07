package io.fekav.req.syntaxextraction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.extraction.application.ExtractSyntaxCommand;
import io.fekav.req.extraction.application.ExtractSyntaxCommandHandler;
import io.fekav.req.extraction.application.SyntaxExtraction;
import io.fekav.req.extraction.domain.Action;
import io.fekav.req.extraction.domain.Subject;
import io.fekav.req.extraction.domain.TargetObject;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.RawText;

class ExtractEntitiesCommandHandlerTest {

    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final SyntaxExtraction syntaxExtraction =
        mock(SyntaxExtraction.class);
    private final ExtractSyntaxCommandHandler handler = new ExtractSyntaxCommandHandler(
        eventPublisher,
        syntaxExtraction
    );

    @Test
    void returnsSyntaxExtractedEventAndPublishesSameEvent_whenExtractionSucceeds() {
        CorrelationId correlationId = CorrelationId.create();
        String rawText = "The reporting dashboard shall export monthly usage metrics.";
        Action action = action();
        when(syntaxExtraction.extractSyntax(new RawText(rawText)))
            .thenReturn(action);

        RequirementElementsExtractedEvent result =
            handler.handle(new ExtractSyntaxCommand(correlationId, rawText));

        assertThat(result.correlationId()).isEqualTo(correlationId);
        assertThat(result.rawText()).isEqualTo(new RawText(rawText));
        assertThat(result.action()).isEqualTo(action);
        verify(syntaxExtraction).extractSyntax(new RawText(rawText));
        verify(eventPublisher).publish(result);
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
