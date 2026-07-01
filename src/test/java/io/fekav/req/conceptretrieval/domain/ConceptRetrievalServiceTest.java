package io.fekav.req.conceptretrieval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.SelectedTerm;

class ConceptRetrievalServiceTest {

    @Test
    void returnsMatchForSelectedTerm() {
        // Given
        List<SelectedTerm> retrievedTerms = new ArrayList<>();
        ConceptRetrievalPolicy policy = selectedTerm -> {
            retrievedTerms.add(selectedTerm);
            return noMatch(selectedTerm);
        };
        ConceptRetrievalService service = new ConceptRetrievalService(policy);
        SelectedTerm selectedTerm = new SelectedTerm(RequirementElement.SUBJECT, "billing service");

        // When
        CandidateConceptMatch match = service.retrieveCandidates(selectedTerm);

        // Then
        assertThat(retrievedTerms).containsExactly(selectedTerm);
        assertThat(match.selectedTerm()).isEqualTo(selectedTerm);
    }

    @Test
    void rejectsRetrievalRequest_whenSelectedTermIsNull() {
        // Given
        ConceptRetrievalService service = new ConceptRetrievalService(this::noMatch);

        // When / Then
        assertThatThrownBy(() -> service.retrieveCandidates(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("selectedTerm must not be null");
    }

    @Test
    void rejectsRetrievalRequest_whenPolicyDoesNotReturnMatchingTerm() {
        // Given
        ConceptRetrievalService service = new ConceptRetrievalService(
            selectedTerm -> noMatch(new SelectedTerm(RequirementElement.ACTION, "refund"))
        );
        SelectedTerm selectedTerm = new SelectedTerm(RequirementElement.SUBJECT, "billing service");

        // When / Then
        assertThatThrownBy(() -> service.retrieveCandidates(selectedTerm))
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
