package io.fekav.req.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.fekav.req.classification.domain.Classification;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.Rationale;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.classification.domain.RequirementType;
import io.fekav.req.shared.event.SyntaxExtractedEvent;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.syntaxextraction.domain.Action;
import io.fekav.req.syntaxextraction.domain.Subject;
import io.fekav.req.syntaxextraction.domain.TargetObject;

class RequirementTest {

    @Test
    void applyExtractionSetsStatusToExtractedAndPublishesEvent() {
        Requirement requirement = Requirement.create("The dashboard shall export metrics.");
        Action action = action();

        requirement.applyExtraction(action);

        assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.EXTRACTED);
        assertThat(requirement.domainEvents())
            .singleElement()
            .satisfies(domainEvent -> {
                assertThat(domainEvent).isInstanceOf(SyntaxExtractedEvent.class);
                SyntaxExtractedEvent event = (SyntaxExtractedEvent) domainEvent;
                assertThat(event.requirementId()).isEqualTo(requirement.getId());
                assertThat(event.action()).isEqualTo(action);
            });
    }

    @Test
    void applyExtractionRejectsNullAction() {
        Requirement requirement = Requirement.create("The dashboard shall export metrics.");

        assertThatThrownBy(() -> requirement.applyExtraction(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("action must not be null");
    }

    @Test
    void applyClassificationBehaviorIsUnchanged() {
        Requirement requirement = Requirement.create("The checkout service must support guest checkout.");
        Classification classification = new Classification(
            RequirementType.REQUIREMENT,
            RequirementProperty.FUNCTIONAL,
            new ConfidenceScore(0.94),
            new Rationale("The text assigns a verifiable obligation to the service.")
        );

        requirement.applyClassification(classification);

        assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.CLASSIFIED);
        assertThat(requirement.domainEvents())
            .singleElement()
            .satisfies(domainEvent -> {
                assertThat(domainEvent).isInstanceOf(RequirementClassifiedEvent.class);
                RequirementClassifiedEvent event = (RequirementClassifiedEvent) domainEvent;
                assertThat(event.requirementId()).isEqualTo(requirement.getId());
                assertThat(event.classification()).isEqualTo(classification);
            });
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
