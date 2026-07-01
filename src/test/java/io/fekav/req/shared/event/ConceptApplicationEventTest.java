package io.fekav.req.shared.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.DomainEvent;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecisionStatus;
import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.SelectedTerm;

class ConceptApplicationEventTest {

    @Test
    void createConceptCandidatesRetrievedEvent_preservesMatchWithCandidatesAndSetsMetadata() {
        // Given
        CandidateConceptMatch match = matchWithCandidates(
            new SelectedTerm(RequirementElement.SUBJECT, "billing service"),
            candidate("concept-1")
        );

        // When
        ConceptCandidatesRetrievedEvent event = ConceptCandidatesRetrievedEvent.create(match);

        // Then
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.match()).isEqualTo(match);
    }

    @Test
    void createConceptCandidatesRetrievedEvent_preservesMatchWithoutCandidates() {
        // Given
        CandidateConceptMatch match = new CandidateConceptMatch(
            new SelectedTerm(RequirementElement.SUBJECT, "billing service"),
            List.of()
        );

        // When
        ConceptCandidatesRetrievedEvent event = ConceptCandidatesRetrievedEvent.create(match);

        // Then
        assertThat(event.match()).isEqualTo(match);
        assertThat(event.match().candidates()).isEmpty();
    }

    @Test
    void createConceptMatchEvaluatedEvent_preservesMatchAndDecisionWithMetadata() {
        // Given
        CandidateConceptMatch match = matchWithCandidates(
            new SelectedTerm(RequirementElement.SUBJECT, "billing service"),
            candidate("concept-1")
        );
        ConceptMatchDecision decision = autoMapExistingDecision();

        // When
        ConceptMatchEvaluatedEvent event = ConceptMatchEvaluatedEvent.create(match, decision);

        // Then
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.match()).isEqualTo(match);
        assertThat(event.decision()).isEqualTo(decision);
    }

    @Test
    void createMapExistingConceptRequestedEvent_capturesMappingRequestFact() {
        // Given
        ConceptMatchDecision decision = autoMapExistingDecision();

        // When
        MapExistingConceptRequestedEvent event =
            MapExistingConceptRequestedEvent.create(decision);

        // Then
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.selectedTerm()).isEqualTo(decision.selectedTerm());
        assertThat(event.existingConcept()).isEqualTo(decision.candidates().getFirst().candidate());
        assertThat(event.rationale()).isEqualTo(decision.rationale());
    }

    @Test
    void createMapExistingConceptRequestedEvent_rejectsWrongDecisionStatus() {
        // Given
        ConceptMatchDecision decision = proposeExistingDecision();

        // When / Then
        assertThatThrownBy(() -> MapExistingConceptRequestedEvent.create(decision))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("map existing concept requested event requires AUTO_MAP_EXISTING status");
    }

    @Test
    void createExistingConceptProposedEvent_capturesProposedConceptFact() {
        // Given
        ConceptMatchDecision decision = proposeExistingDecision();

        // When
        ExistingConceptProposedEvent event =
            ExistingConceptProposedEvent.create(decision);

        // Then
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.selectedTerm()).isEqualTo(decision.selectedTerm());
        assertThat(event.existingConcept()).isEqualTo(decision.candidates().getFirst().candidate());
        assertThat(event.rationale()).isEqualTo(decision.rationale());
    }

    @Test
    void createExistingConceptProposedEvent_rejectsWrongDecisionStatus() {
        // Given
        ConceptMatchDecision decision = autoMapExistingDecision();

        // When / Then
        assertThatThrownBy(() -> ExistingConceptProposedEvent.create(decision))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("existing concept proposed event requires PROPOSE_EXISTING status");
    }

    @Test
    void createConceptMatchReviewRequestedEvent_capturesReviewRequestFact() {
        // Given
        ConceptMatchDecision decision = reviewRequiredDecision();

        // When
        ConceptMatchReviewRequestedEvent event =
            ConceptMatchReviewRequestedEvent.create(decision);

        // Then
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.selectedTerm()).isEqualTo(decision.selectedTerm());
        assertThat(event.rationale()).isEqualTo(decision.rationale());
    }

    @Test
    void createConceptMatchReviewRequestedEvent_rejectsWrongDecisionStatus() {
        // Given
        ConceptMatchDecision decision = proposeExistingDecision();

        // When / Then
        assertThatThrownBy(() -> ConceptMatchReviewRequestedEvent.create(decision))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("concept match review requested event requires REVIEW_REQUIRED status");
    }

    @Test
    void createCreateConceptRequestedEvent_capturesCreationRequestFact() {
        // Given
        ConceptMatchDecision decision = autoCreateNewDecision();

        // When
        CreateConceptRequestedEvent event =
            CreateConceptRequestedEvent.create(decision);

        // Then
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.selectedTerm()).isEqualTo(decision.selectedTerm());
        assertThat(event.rationale()).isEqualTo(decision.rationale());
    }

    @Test
    void createCreateConceptRequestedEvent_rejectsWrongDecisionStatus() {
        // Given
        ConceptMatchDecision decision = reviewRequiredDecision();

        // When / Then
        assertThatThrownBy(() -> CreateConceptRequestedEvent.create(decision))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("create concept requested event requires AUTO_CREATE_NEW status");
    }

    private ConceptMatchDecision autoMapExistingDecision() {
        return existingConceptDecision(
            new SelectedTerm(RequirementElement.SUBJECT, "billing service"),
            ConceptMatchDecisionStatus.AUTO_MAP_EXISTING,
            "Best candidate exceeded the auto-map threshold"
        );
    }

    private ConceptMatchDecision proposeExistingDecision() {
        return existingConceptDecision(
            new SelectedTerm(RequirementElement.ACTION, "refund payment"),
            ConceptMatchDecisionStatus.PROPOSE_EXISTING,
            "Best candidate was proposed"
        );
    }

    private ConceptMatchDecision reviewRequiredDecision() {
        return new ConceptMatchDecision(
            new SelectedTerm(RequirementElement.OBJECT, "customer account"),
            ConceptMatchDecisionStatus.REVIEW_REQUIRED,
            List.of(candidate("concept-1"), candidate("concept-2")),
            "Multiple candidates require review"
        );
    }

    private ConceptMatchDecision autoCreateNewDecision() {
        SelectedTerm selectedTerm = new SelectedTerm(RequirementElement.CONDITION, "after timeout");
        return new ConceptMatchDecision(
            selectedTerm,
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        );
    }

    private ConceptMatchDecision existingConceptDecision(
        SelectedTerm selectedTerm,
        ConceptMatchDecisionStatus status,
        String rationale
    ) {
        return new ConceptMatchDecision(
            selectedTerm,
            status,
            List.of(candidate("concept-" + selectedTerm.requirementElement().name().toLowerCase())),
            rationale
        );
    }

    private CandidateConceptMatch matchWithCandidates(
        SelectedTerm selectedTerm,
        RetrievedCandidateConcept candidate
    ) {
        return new CandidateConceptMatch(selectedTerm, List.of(candidate));
    }

    private RetrievedCandidateConcept candidate(String candidateKey) {
        return new RetrievedCandidateConcept(
            new CandidateConcept(candidateKey, "Billing Service", "SystemComponent"),
            List.of(new RetrievalEvidence("conceptName", "matched concept name", 1.0))
        );
    }
}
