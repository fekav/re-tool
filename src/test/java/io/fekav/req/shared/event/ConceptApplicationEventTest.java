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
import io.fekav.req.shared.model.CorrelationId;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.RequirementElement;

class ConceptApplicationEventTest {

    @Test
    void createConceptCandidatesRetrievedEvent_preservesMatchWithCandidatesAndSetsMetadata() {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        CandidateConceptMatch match = matchWithCandidates(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            candidate("concept-1")
        );

        // When
        ConceptCandidatesRetrievedEvent event = ConceptCandidatesRetrievedEvent.create(correlationId, match);

        // Then
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.match()).isEqualTo(match);
    }

    @Test
    void createConceptCandidatesRetrievedEvent_preservesMatchWithoutCandidates() {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        CandidateConceptMatch match = new CandidateConceptMatch(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            List.of()
        );

        // When
        ConceptCandidatesRetrievedEvent event = ConceptCandidatesRetrievedEvent.create(correlationId, match);

        // Then
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.match()).isEqualTo(match);
        assertThat(event.match().candidates()).isEmpty();
    }

    @Test
    void createConceptMatchEvaluatedEvent_preservesMatchAndDecisionWithMetadata() {
        // Given
        CorrelationId correlationId = CorrelationId.create();
        CandidateConceptMatch match = matchWithCandidates(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            candidate("concept-1")
        );
        ConceptMatchDecision decision = autoMapExistingDecision();

        // When
        ConceptMatchEvaluatedEvent event = ConceptMatchEvaluatedEvent.create(correlationId, match, decision);

        // Then
        assertThat(event).isInstanceOf(ApplicationEvent.class);
        assertThat(event).isNotInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.match()).isEqualTo(match);
        assertThat(event.decision()).isEqualTo(decision);
    }

    private ConceptMatchDecision autoMapExistingDecision() {
        return existingConceptDecision(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            ConceptMatchDecisionStatus.AUTO_MAP_EXISTING,
            "Best candidate exceeded the auto-map threshold"
        );
    }

    private ConceptMatchDecision proposeExistingDecision() {
        return existingConceptDecision(
            new RequirementElement(RequirementElementType.ACTION, "refund payment"),
            ConceptMatchDecisionStatus.PROPOSE_EXISTING,
            "Best candidate was proposed"
        );
    }

    private ConceptMatchDecision reviewRequiredDecision() {
        return new ConceptMatchDecision(
            new RequirementElement(RequirementElementType.OBJECT, "customer account"),
            ConceptMatchDecisionStatus.REVIEW_REQUIRED,
            List.of(candidate("concept-1"), candidate("concept-2")),
            "Multiple candidates require review"
        );
    }

    private ConceptMatchDecision autoCreateNewDecision() {
        RequirementElement selectedTerm = new RequirementElement(RequirementElementType.CONDITION, "after timeout");
        return new ConceptMatchDecision(
            selectedTerm,
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        );
    }

    private ConceptMatchDecision existingConceptDecision(
        RequirementElement requirementElement,
        ConceptMatchDecisionStatus status,
        String rationale
    ) {
        return new ConceptMatchDecision(
            requirementElement,
            status,
            List.of(candidate("concept-" + requirementElement.type().name().toLowerCase())),
            rationale
        );
    }

    private CandidateConceptMatch matchWithCandidates(
        RequirementElement selectedTerm,
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
