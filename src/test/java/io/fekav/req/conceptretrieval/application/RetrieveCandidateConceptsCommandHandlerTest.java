package io.fekav.req.conceptretrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalPolicy;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalService;
import io.fekav.req.shared.event.ConceptCandidatesRetrievedEvent;
import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.SelectedTerm;

@ExtendWith(MockitoExtension.class)
class RetrieveCandidateConceptsCommandHandlerTest {

    @Mock
    EventPublisher eventPublisher;

    @Test
    void returnsConceptCandidatesRetrievedEventAndPublishesSameEvent_whenRetrievalSucceeds() {
        // Given
        List<SelectedTerm> retrievedTerms = new ArrayList<>();
        ConceptRetrievalPolicy policy = selectedTerm -> {
            retrievedTerms.add(selectedTerm);
            return match(selectedTerm, List.of(candidate("concept-1")));
        };
        RetrieveCandidateConceptsCommandHandler handler =
            handlerWith(policy);
        RetrieveCandidateConceptsCommand command = new RetrieveCandidateConceptsCommand(
            new SelectedTerm(RequirementElement.SUBJECT,
                " billing service "
            )
        );

        // When
        ConceptCandidatesRetrievedEvent result = handler.handle(command);

        // Then
        assertThat(retrievedTerms)
            .containsExactly(new SelectedTerm(RequirementElement.SUBJECT, "billing service"));
        assertThat(result.match().selectedTerm())
            .isEqualTo(new SelectedTerm(RequirementElement.SUBJECT, "billing service"));
        assertThat(result.match().candidates())
            .singleElement()
            .satisfies(candidate ->
                assertThat(candidate.candidate().candidateKey()).isEqualTo("concept-1")
            );
        verify(eventPublisher).publish(result);
    }

    @Test
    void returnsEventWithEmptyCandidates_whenRetrievedMatchHasNoCandidates() {
        // Given
        RetrieveCandidateConceptsCommandHandler handler =
            handlerWith(this::noMatch);
        RetrieveCandidateConceptsCommand command = new RetrieveCandidateConceptsCommand(
            new SelectedTerm(RequirementElement.ACTION, "must refund")
        );

        // When
        ConceptCandidatesRetrievedEvent result = handler.handle(command);

        // Then
        assertThat(result.match().selectedTerm())
            .isEqualTo(new SelectedTerm(RequirementElement.ACTION, "must refund"));
        assertThat(result.match().candidates()).isEmpty();
        verify(eventPublisher).publish(result);
    }

    @Test
    void rejectsCommand_whenSelectedTermIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new RetrieveCandidateConceptsCommand(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("selectedTerm must not be null");
    }

    @Test
    void rejectsSelectedTerm_whenTextIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new SelectedTerm(RequirementElement.SUBJECT, " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("selected term text must not be blank");
    }

    @Test
    void returnsCommandType() {
        // Given
        RetrieveCandidateConceptsCommandHandler handler =
            handlerWith(this::noMatch);

        // When
        Class<RetrieveCandidateConceptsCommand> commandType = handler.commandType();

        // Then
        assertThat(commandType).isEqualTo(RetrieveCandidateConceptsCommand.class);
    }

    private CandidateConceptMatch noMatch(SelectedTerm selectedTerm) {
        return match(selectedTerm, List.of());
    }

    private RetrieveCandidateConceptsCommandHandler handlerWith(ConceptRetrievalPolicy policy) {
        return new RetrieveCandidateConceptsCommandHandler(
            eventPublisher,
            new ConceptRetrievalService(policy)
        );
    }

    private CandidateConceptMatch match(
        SelectedTerm selectedTerm,
        List<RetrievedCandidateConcept> candidates
    ) {
        return new CandidateConceptMatch(selectedTerm, candidates);
    }

    private RetrievedCandidateConcept candidate(String candidateKey) {
        return new RetrievedCandidateConcept(
            new CandidateConcept(candidateKey, "Billing Service", "SystemComponent"),
            List.of(new RetrievalEvidence("conceptName", "matched concept name", 1.0))
        );
    }
}
