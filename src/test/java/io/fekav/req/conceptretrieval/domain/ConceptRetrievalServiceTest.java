package io.fekav.req.conceptretrieval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.CandidateConceptMatchSet;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.SelectedTerm;

class ConceptRetrievalServiceTest {

    @Test
    void returnsOneMatchForEachSelectedTerm() {
        // Given
        List<SelectedTerm> retrievedTerms = new ArrayList<>();
        ConceptRetrievalPolicy policy = selectedTerm -> {
            retrievedTerms.add(selectedTerm);
            return noMatch(selectedTerm);
        };
        ConceptRetrievalService service = new ConceptRetrievalService(policy);
        List<SelectedTerm> selectedTerms = List.of(
            new SelectedTerm(RequirementElement.SUBJECT, "billing service"),
            new SelectedTerm(RequirementElement.ACTION, "refund")
        );

        // When
        CandidateConceptMatchSet matchSet = service.retrieveCandidates(selectedTerms);

        // Then
        assertThat(retrievedTerms).containsExactlyElementsOf(selectedTerms);
        assertThat(matchSet.matches())
            .extracting(CandidateConceptMatch::selectedTerm)
            .containsExactlyElementsOf(selectedTerms);
    }

    @Test
    void rejectsRetrievalRequest_whenSelectedTermsAreEmpty() {
        // Given
        ConceptRetrievalService service = new ConceptRetrievalService(this::noMatch);

        // When / Then
        assertThatThrownBy(() -> service.retrieveCandidates(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("selected terms must not be empty");
    }

    @Test
    void rejectsRetrievalRequest_whenPolicyDoesNotReturnMatchingTerm() {
        // Given
        ConceptRetrievalService service = new ConceptRetrievalService(
            selectedTerm -> noMatch(new SelectedTerm(RequirementElement.ACTION, "refund"))
        );

        // When / Then
        assertThatThrownBy(() -> service.retrieveCandidates(List.of(
            new SelectedTerm(RequirementElement.SUBJECT, "billing service")
        )))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("retrieval policy returned a match for a different selected term");
    }

    private CandidateConceptMatch noMatch(SelectedTerm selectedTerm) {
        return new CandidateConceptMatch(
            selectedTerm,
            List.of()
        );
    }
}
