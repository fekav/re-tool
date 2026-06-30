package io.fekav.req.conceptretrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.conceptretrieval.domain.ConceptRetrievalPolicy;
import io.fekav.req.conceptretrieval.domain.ConceptRetrievalService;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.CandidateConceptMatchSet;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.SelectedTerm;

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
