package io.fekav.req.conceptretrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.conceptretrieval.domain.CandidateConceptMatch;
import io.fekav.req.conceptretrieval.domain.CandidateConceptMatchSet;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalPolicy;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalService;
import io.fekav.req.conceptretrieval.domain.SelectedTerm;

class RetrieveCandidateConceptsCommandHandlerTest {

    @Test
    void returnsCandidateConceptMatchSet_whenRetrievalSucceeds() {
        // Given
        List<SelectedTerm> retrievedTerms = new ArrayList<>();
        ConceptRetrievalPolicy policy = selectedTerm -> {
            retrievedTerms.add(selectedTerm);
            return noMatch(selectedTerm);
        };
        RetrieveCandidateConceptsCommandHandler handler =
            new RetrieveCandidateConceptsCommandHandler(new ConceptRetrievalService(policy));
        RetrieveCandidateConceptsCommand command = new RetrieveCandidateConceptsCommand(List.of(
            new RetrieveCandidateConceptsCommand.SelectedTermInput(
                " SUBJECT ",
                " billing service "
            ),
            new RetrieveCandidateConceptsCommand.SelectedTermInput(
                "ACTION",
                "must refund"
            )
        ));

        // When
        CandidateConceptMatchSet result = handler.handle(command);

        // Then
        assertThat(result.matches()).hasSize(2);
        assertThat(retrievedTerms)
            .containsExactly(
                new SelectedTerm("SUBJECT", "billing service"),
                new SelectedTerm("ACTION", "must refund")
            );
    }

    @Test
    void rejectsCommand_whenSelectedTermInputsAreEmpty() {
        // Given / When / Then
        assertThatThrownBy(() -> new RetrieveCandidateConceptsCommand(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("retrieve candidate concepts command has no selected terms");
    }

    @Test
    void rejectsCommand_whenSelectedTermInputIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new RetrieveCandidateConceptsCommand(List.of(
            new RetrieveCandidateConceptsCommand.SelectedTermInput("SUBJECT", " ")
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("selected term input text must not be blank");
    }

    @Test
    void rejectsCommand_whenSelectedTermInputIsDuplicated() {
        // Given
        RetrieveCandidateConceptsCommand.SelectedTermInput selectedTerm =
            new RetrieveCandidateConceptsCommand.SelectedTermInput(
                "SUBJECT",
                "billing service"
            );

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
            new RetrieveCandidateConceptsCommandHandler(
                new ConceptRetrievalService(this::noMatch)
            );

        // When
        Class<RetrieveCandidateConceptsCommand> commandType = handler.commandType();

        // Then
        assertThat(commandType).isEqualTo(RetrieveCandidateConceptsCommand.class);
    }

    private CandidateConceptMatch noMatch(SelectedTerm selectedTerm) {
        return new CandidateConceptMatch(
            selectedTerm,
            List.of()
        );
    }
}
