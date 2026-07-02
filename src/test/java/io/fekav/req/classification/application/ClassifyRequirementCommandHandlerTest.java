package io.fekav.req.classification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.classification.domain.Rationale;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.classification.domain.RequirementType;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.model.CorrelationId;
import io.fekav.req.shared.model.RawText;

class ClassifyRequirementCommandHandlerTest {

    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final ClassificationService requirementClassificationService =
        mock(ClassificationService.class);
    private final ClassifyRequirementCommandHandler handler = new ClassifyRequirementCommandHandler(
        eventPublisher,
        requirementClassificationService
    );

    @Test
    void returnsRequirementClassifiedEventAndPublishesSameEvent_whenClassificationSucceeds() {
        CorrelationId correlationId = CorrelationId.create();
        String rawText = "The checkout page must load within 2 seconds on a 4G connection.";
        Classification classification = requirementClassification(
            RequirementType.REQUIREMENT,
            RequirementProperty.QUALITY,
            0.93,
            "The sentence uses must and gives a measurable response-time constraint."
        );
        when(requirementClassificationService.classifyRequirement(new RawText(rawText)))
            .thenReturn(classification);

        RequirementClassifiedEvent result = handler.handle(
            new ClassifyRequirementCommand(correlationId, rawText)
        );

        assertThat(result.correlationId()).isEqualTo(correlationId);
        assertThat(result.rawText()).isEqualTo(new RawText(rawText));
        assertThat(result.classification()).isEqualTo(classification);
        verify(requirementClassificationService).classifyRequirement(new RawText(rawText));
        verify(eventPublisher).publish(result);
    }

    @Test
    void preservesLowConfidenceClassification() {
        String rawText = "Make checkout better for returning customers.";
        Classification classification = requirementClassification(
            RequirementType.GOAL,
            RequirementProperty.FUNCTIONAL,
            0.42,
            "The wording is ambiguous, so this is a forced best-fit classification."
        );
        when(requirementClassificationService.classifyRequirement(new RawText(rawText)))
            .thenReturn(classification);

        RequirementClassifiedEvent result = handler.handle(new ClassifyRequirementCommand(rawText));

        assertThat(result.classification().confidenceScore()).isEqualTo(new ConfidenceScore(0.42));
    }

    @Test
    void rejectsClassifyRequirementCommand_whenRawTextIsBlank() {
        assertThatThrownBy(() -> new ClassifyRequirementCommand(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Classification command has no raw text");
    }

    private Classification requirementClassification(
        RequirementType conceptType,
        RequirementProperty property,
        double confidenceScore,
        String rationale
    ) {
        return new Classification(
            conceptType,
            property,
            new ConfidenceScore(confidenceScore),
            new Rationale(rationale)
        );
    }
}
