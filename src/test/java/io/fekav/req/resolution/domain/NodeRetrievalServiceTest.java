package io.fekav.req.resolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RequirementElement;

class NodeRetrievalServiceTest {

    @Test
    void returnsMatchForSelectedTerm() {
        // Given
        List<RequirementElement> retrievedTerms = new ArrayList<>();
        NodeRetrievalPolicy policy = selectedTerm -> {
            retrievedTerms.add(selectedTerm);
            return noMatch(selectedTerm);
        };
        NodeRetrievalService service = new NodeRetrievalService(policy);
        RequirementElement selectedTerm = new RequirementElement(RequirementElementType.SUBJECT, "billing service");

        // When
        CandidateNodeMatch match = service.retrieveCandidates(selectedTerm);

        // Then
        assertThat(retrievedTerms).containsExactly(selectedTerm);
        assertThat(match.requirementElement()).isEqualTo(selectedTerm);
    }

    @Test
    void rejectsRetrievalRequest_whenSelectedTermIsNull() {
        // Given
        NodeRetrievalService service = new NodeRetrievalService(this::noMatch);

        // When / Then
        assertThatThrownBy(() -> service.retrieveCandidates(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("selectedTerm must not be null");
    }

    @Test
    void rejectsRetrievalRequest_whenPolicyDoesNotReturnMatchingTerm() {
        // Given
        NodeRetrievalService service = new NodeRetrievalService(
            selectedTerm -> noMatch(new RequirementElement(RequirementElementType.ACTION, "refund"))
        );
        RequirementElement selectedTerm = new RequirementElement(RequirementElementType.SUBJECT, "billing service");

        // When / Then
        assertThatThrownBy(() -> service.retrieveCandidates(selectedTerm))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("retrieval policy returned a match for a different selected term");
    }

    private CandidateNodeMatch noMatch(RequirementElement selectedTerm) {
        return new CandidateNodeMatch(
            selectedTerm,
            List.of()
        );
    }
}
