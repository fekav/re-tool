package io.fekav.req.classification.application;

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
import io.fekav.req.classification.domain.ClassificationRationale;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.RequirementClassification;
import io.fekav.req.classification.domain.RequirementConceptType;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.model.RawText;

class ClassifyRequirementCommandHandlerTest {

    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final RequirementClassificationService requirementClassificationService =
        mock(RequirementClassificationService.class);
    private final ClassifyRequirementCommandHandler handler = new ClassifyRequirementCommandHandler(
        eventPublisher,
        requirementClassificationService
    );

    @Test
    void returnsRequirementClassificationAndPublishesEvent_whenClassificationSucceeds() {
        String rawText = "The checkout page must load within 2 seconds on a 4G connection.";
        RequirementClassification classification = requirementClassification(
            RequirementConceptType.REQUIREMENT,
            RequirementProperty.QUALITY,
            0.93,
            "The sentence uses must and gives a measurable response-time constraint."
        );
        when(requirementClassificationService.classifyRequirement(new RawText(rawText)))
            .thenReturn(classification);

        RequirementClassification result = handler.handle(new ClassifyRequirementCommand(rawText));

        assertThat(result).isEqualTo(classification);
        verify(requirementClassificationService).classifyRequirement(new RawText(rawText));

        ArgumentCaptor<List<DomainEvent>> domainEvents = eventCaptor();
        verify(eventPublisher).publishAll(domainEvents.capture());
        assertThat(domainEvents.getValue())
            .singleElement()
            .satisfies(domainEvent -> {
                assertThat(domainEvent).isInstanceOf(RequirementClassifiedEvent.class);
                RequirementClassifiedEvent event = (RequirementClassifiedEvent) domainEvent;
                assertThat(event.classification()).isEqualTo(classification);
            });
    }

    @Test
    void preservesLowConfidenceClassification() {
        String rawText = "Make checkout better for returning customers.";
        RequirementClassification classification = requirementClassification(
            RequirementConceptType.GOAL,
            RequirementProperty.FUNCTIONAL,
            0.42,
            "The wording is ambiguous, so this is a forced best-fit classification."
        );
        when(requirementClassificationService.classifyRequirement(new RawText(rawText)))
            .thenReturn(classification);

        RequirementClassification result = handler.handle(new ClassifyRequirementCommand(rawText));

        assertThat(result.confidenceScore()).isEqualTo(new ConfidenceScore(0.42));
    }

    @Test
    void rejectsClassifyRequirementCommand_whenRawTextIsBlank() {
        assertThatThrownBy(() -> new ClassifyRequirementCommand(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Classification command has no raw text");
    }

    private RequirementClassification requirementClassification(
        RequirementConceptType conceptType,
        RequirementProperty property,
        double confidenceScore,
        String rationale
    ) {
        return new RequirementClassification(
            conceptType,
            property,
            new ConfidenceScore(confidenceScore),
            new ClassificationRationale(rationale)
        );
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<DomainEvent>> eventCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
