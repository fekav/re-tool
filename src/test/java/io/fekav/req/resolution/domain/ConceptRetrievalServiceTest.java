package io.fekav.req.resolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RequirementElement;

class ConceptRetrievalServiceTest {

    @Test
    void returnsMatchForSelectedTerm() {
        // Given
        List<RequirementElement> retrievedTerms = new ArrayList<>();
        ConceptRetrievalPolicy policy = selectedTerm -> {
            retrievedTerms.add(selectedTerm);
            return noMatch(selectedTerm);
        };
        ConceptRetrievalService service = new ConceptRetrievalService(policy);
        RequirementElement selectedTerm = new RequirementElement(RequirementElementType.SUBJECT, "billing service");

        // When
        CandidateConceptMatch match = service.retrieveCandidates(selectedTerm);

        // Then
        assertThat(retrievedTerms).containsExactly(selectedTerm);
        assertThat(match.requirementElement()).isEqualTo(selectedTerm);
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
            selectedTerm -> noMatch(new RequirementElement(RequirementElementType.ACTION, "refund"))
        );
        RequirementElement selectedTerm = new RequirementElement(RequirementElementType.SUBJECT, "billing service");

        // When / Then
        assertThatThrownBy(() -> service.retrieveCandidates(selectedTerm))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("retrieval policy returned a match for a different selected term");
    }

    private CandidateConceptMatch noMatch(RequirementElement selectedTerm) {
        return new CandidateConceptMatch(
            selectedTerm,
            List.of()
        );
    }
}
