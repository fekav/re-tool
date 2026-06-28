package io.fekav.req.conceptretrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.fekav.req.conceptretrieval.domain.CandidateConcept;
import io.fekav.req.conceptretrieval.domain.CandidateConceptMatch;
import io.fekav.req.conceptretrieval.domain.CandidateConceptMatchSet;
import io.fekav.req.conceptretrieval.domain.RetrievalEvidence;

@ExtendWith(MockitoExtension.class)
class RetrieveCandidateConceptsCommandHandlerTest {

    @Mock
    ConceptRetrievalPolicy conceptRetrievalPolicy;

    @InjectMocks
    RetrieveCandidateConceptsCommandHandler handler;

    @Test
    void returnsCandidateMatchSet_whenPolicyRetrievesCandidates() {
        // Given
        RetrieveCandidateConceptsCommand command = new RetrieveCandidateConceptsCommand(
            List.of(
                new RetrieveCandidateConceptsCommand.SelectedTermInput(
                    " SUBJECT ",
                    " reporting dashboard "
                ),
                new RetrieveCandidateConceptsCommand.SelectedTermInput(
                    "OBJECT",
                    "monthly usage metrics"
                )
            )
        );
        CandidateConceptMatchSet expected = matchSet(
            new RetrieveCandidateConceptsCommand.SelectedTermInput(
                "SUBJECT",
                "reporting dashboard"
            ),
            new RetrieveCandidateConceptsCommand.SelectedTermInput(
                "OBJECT",
                "monthly usage metrics"
            )
        );
        when(conceptRetrievalPolicy.retrieveCandidates(anyCollection()))
            .thenReturn(expected);

        // When
        CandidateConceptMatchSet result = handler.handle(command);

        // Then
        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<Collection<RetrieveCandidateConceptsCommand.SelectedTermInput>> terms =
            selectedTermsCaptor();
        verify(conceptRetrievalPolicy).retrieveCandidates(terms.capture());
        assertThat(terms.getValue())
            .extracting(
                RetrieveCandidateConceptsCommand.SelectedTermInput::syntaxRole,
                RetrieveCandidateConceptsCommand.SelectedTermInput::text
            )
            .containsExactly(
                tuple(
                    "SUBJECT",
                    "reporting dashboard"
                ),
                tuple(
                    "OBJECT",
                    "monthly usage metrics"
                )
            );
        verifyNoMoreInteractions(conceptRetrievalPolicy);
    }

    @Test
    void returnsCommandType() {
        // When
        Class<RetrieveCandidateConceptsCommand> commandType = handler.commandType();

        // Then
        assertThat(commandType).isEqualTo(RetrieveCandidateConceptsCommand.class);
    }

    @Test
    void rejectsCommand_whenSelectedTermsAreEmpty() {
        // When / Then
        assertThatThrownBy(() -> new RetrieveCandidateConceptsCommand(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Retrieve candidate concepts command has no selected terms");
    }

    @Test
    void rejectsCommand_whenSelectedTermIsNull() {
        // Given
        List<RetrieveCandidateConceptsCommand.SelectedTermInput> selectedTerms =
            new ArrayList<>();
        selectedTerms.add(null);

        // When / Then
        assertThatThrownBy(() -> new RetrieveCandidateConceptsCommand(selectedTerms))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("selected term input must not be null");
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Collection<RetrieveCandidateConceptsCommand.SelectedTermInput>>
            selectedTermsCaptor() {
        return ArgumentCaptor.forClass(Collection.class);
    }

    private CandidateConceptMatchSet matchSet(
        RetrieveCandidateConceptsCommand.SelectedTermInput... terms
    ) {
        return new CandidateConceptMatchSet(
            java.util.Arrays.stream(terms)
                .map(term -> new CandidateConceptMatch(
                    term.syntaxRole(),
                    term.text(),
                    List.of(new CandidateConcept(
                        "concept-" + term.syntaxRole().toLowerCase(),
                        term.text(),
                        "SystemComponent"
                    )),
                    List.of(new RetrievalEvidence(
                        "exactMatch",
                        "label = " + term.text(),
                        1.0
                    ))
                ))
                .toList()
        );
    }
}
