package io.fekav.req.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.fekav.req.classification.domain.Classification;
import io.fekav.req.classification.domain.ConfidenceScore;
import io.fekav.req.classification.domain.Rationale;
import io.fekav.req.classification.domain.RequirementProperty;
import io.fekav.req.classification.domain.RequirementType;
import io.fekav.req.syntaxextraction.domain.Action;
import io.fekav.req.syntaxextraction.domain.Subject;
import io.fekav.req.syntaxextraction.domain.TargetObject;

class RequirementTest {

    @Test
    void applyProvenanceSetsStatusToIngested() {
        // Arrange
        Requirement requirement = Requirement.create("The dashboard shall export metrics.");
        Provenance provenance = provenance("The dashboard shall export metrics.");

        // Act
        requirement.applyProvenance(provenance);

        // Assert
        assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.INGESTED);
        assertThat(requirement.getProvenance()).isEqualTo(provenance);
        assertThat(requirement.domainEvents()).isEmpty();
    }

    @Test
    void applyProvenanceRejectsDuplicateProvenance() {
        // Arrange
        Requirement requirement = Requirement.create("The dashboard shall export metrics.");
        requirement.applyProvenance(provenance("The dashboard shall export metrics."));

        // Act / Assert
        assertThatThrownBy(() -> requirement.applyProvenance(
            provenance("The dashboard shall export metrics.")
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("provenance already applied");
    }

    @Test
    void applyExtractionSetsStatusToExtracted() {
        // Arrange
        Requirement requirement = Requirement.create("The dashboard shall export metrics.");
        Action action = action();

        // Act
        requirement.applyExtraction(action);

        // Assert
        assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.EXTRACTED);
        assertThat(requirement.domainEvents()).isEmpty();
    }

    @Test
    void applyExtractionRejectsNullAction() {
        // Arrange
        Requirement requirement = Requirement.create("The dashboard shall export metrics.");

        // Act / Assert
        assertThatThrownBy(() -> requirement.applyExtraction(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("action must not be null");
    }

    @Test
    void applyClassificationSetsStatusToClassified() {
        // Arrange
        Requirement requirement = Requirement.create("The checkout service must support guest checkout.");
        Classification classification = new Classification(
            RequirementType.REQUIREMENT,
            RequirementProperty.FUNCTIONAL,
            new ConfidenceScore(0.94),
            new Rationale("The text assigns a verifiable obligation to the service.")
        );

        // Act
        requirement.applyClassification(classification);

        // Assert
        assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.CLASSIFIED);
        assertThat(requirement.domainEvents()).isEmpty();
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

    private Provenance provenance(String originalText) {
        return Provenance.create(
            ElementId.create(),
            new OriginalText(originalText),
            SourceMetadata.apiRequest(),
            Instant.parse("2026-07-02T10:15:30Z")
        );
    }
}
