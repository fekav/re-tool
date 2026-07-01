package io.fekav.req.conceptretrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalPolicy;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalService;
import io.fekav.req.shared.event.ConceptCandidatesReadyEvent;
import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.CandidateConceptMatchSet;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.SelectedTerm;

@ExtendWith(MockitoExtension.class)
class RetrieveCandidateConceptsCommandHandlerTest {

    @Mock
    EventPublisher eventPublisher;

    @Test
    void returnsCandidateConceptMatchSetAndPublishesEvents_whenRetrievalSucceeds() {
        // Given
        List<SelectedTerm> retrievedTerms = new ArrayList<>();
        ConceptRetrievalPolicy policy = selectedTerm -> {
            retrievedTerms.add(selectedTerm);
            if (selectedTerm.requirementElement() == RequirementElement.SUBJECT) {
                return match(selectedTerm, List.of(candidate("concept-1")));
            }
            return match(selectedTerm, List.of());
        };
        RetrieveCandidateConceptsCommandHandler handler =
            handlerWith(policy);
        RetrieveCandidateConceptsCommand command = new RetrieveCandidateConceptsCommand(List.of(
            new SelectedTerm(RequirementElement.SUBJECT,
                " billing service "
            ),
            new SelectedTerm(RequirementElement.ACTION,
                "must refund"
            )
        ));

        // When
        CandidateConceptMatchSet result = handler.handle(command);

        // Then
        assertThat(result.matches()).hasSize(2);
        assertThat(retrievedTerms)
            .containsExactly(
                new SelectedTerm(RequirementElement.SUBJECT, "billing service"),
                new SelectedTerm(RequirementElement.ACTION, "must refund")
            );

        ArgumentCaptor<List<ApplicationEvent>> applicationEvents = eventCaptor();
        verify(eventPublisher).publishApplicationEvents(applicationEvents.capture());
        assertThat(applicationEvents.getValue())
            .satisfiesExactly(
                applicationEvent -> {
                    assertThat(applicationEvent).isInstanceOf(ConceptCandidatesReadyEvent.class);
                    ConceptCandidatesReadyEvent event =
                        (ConceptCandidatesReadyEvent) applicationEvent;
                    assertThat(event.match()).isEqualTo(result.matches().getFirst());
                },
                applicationEvent -> {
                    assertThat(applicationEvent).isInstanceOf(ConceptCandidatesReadyEvent.class);
                    ConceptCandidatesReadyEvent event =
                        (ConceptCandidatesReadyEvent) applicationEvent;
                    assertThat(event.match()).isEqualTo(result.matches().get(1));
                    assertThat(event.match().candidates()).isEmpty();
                }
            );
    }

    @Test
    void publishesEventWithEmptyCandidates_whenRetrievedMatchHasNoCandidates() {
        // Given
        RetrieveCandidateConceptsCommandHandler handler =
            handlerWith(this::noMatch);
        RetrieveCandidateConceptsCommand command = new RetrieveCandidateConceptsCommand(List.of(
            new SelectedTerm(RequirementElement.ACTION, "must refund")
        ));

        // When
        CandidateConceptMatchSet result = handler.handle(command);

        // Then
        assertThat(result.matches())
            .singleElement()
            .satisfies(match -> assertThat(match.candidates()).isEmpty());

        ArgumentCaptor<List<ApplicationEvent>> applicationEvents = eventCaptor();
        verify(eventPublisher).publishApplicationEvents(applicationEvents.capture());
        assertThat(applicationEvents.getValue())
            .singleElement()
            .satisfies(applicationEvent -> {
                assertThat(applicationEvent).isInstanceOf(ConceptCandidatesReadyEvent.class);
                ConceptCandidatesReadyEvent event =
                    (ConceptCandidatesReadyEvent) applicationEvent;
                assertThat(event.match()).isEqualTo(result.matches().getFirst());
                assertThat(event.match().candidates()).isEmpty();
            });
    }

    @Test
    void rejectsCommand_whenSelectedTermsAreEmpty() {
        // Given / When / Then
        assertThatThrownBy(() -> new RetrieveCandidateConceptsCommand(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("retrieve candidate concepts command has no selected terms");
    }

    @Test
    void rejectsSelectedTerm_whenTextIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new SelectedTerm(RequirementElement.SUBJECT, " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("selected term text must not be blank");
    }

    @Test
    void rejectsCommand_whenSelectedTermIsDuplicated() {
        // Given
        SelectedTerm selectedTerm = new SelectedTerm(RequirementElement.SUBJECT, "billing service");

        // When / Then
        assertThatThrownBy(() -> new RetrieveCandidateConceptsCommand(List.of(
            selectedTerm,
            selectedTerm
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("retrieve candidate concepts command has duplicate selected terms");
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<List<ApplicationEvent>> eventCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }
}
