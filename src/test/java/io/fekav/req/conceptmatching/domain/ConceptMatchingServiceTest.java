package io.fekav.req.conceptmatching.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.CandidateConceptMatchSet;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.SelectedTerm;

class ConceptMatchingServiceTest {

    @Test
    void returnsOneDecisionForEachCandidateMatch() {
        // Given
        List<CandidateConceptMatch> handledMatches = new ArrayList<>();
        ConceptMatchingPolicy policy = match -> {
            handledMatches.add(match);
            return autoCreateDecision(match.selectedTerm());
        };
        ConceptMatchingService service = new ConceptMatchingService(policy);
        CandidateConceptMatchSet matchSet = new CandidateConceptMatchSet(List.of(
            noMatch(new SelectedTerm(RequirementElement.SUBJECT, "billing service")),
            noMatch(new SelectedTerm(RequirementElement.ACTION, "must refund"))
        ));

        // When
        ConceptMatchDecisionSet result = service.decideMatches(matchSet);

        // Then
        assertThat(handledMatches).containsExactlyElementsOf(matchSet.matches());
        assertThat(result.decisions())
            .extracting(ConceptMatchDecision::selectedTerm)
            .containsExactly(
                new SelectedTerm(RequirementElement.SUBJECT, "billing service"),
                new SelectedTerm(RequirementElement.ACTION, "must refund")
            );
    }

    @Test
    void rejectsDecisionRequest_whenMatchSetIsNull() {
        // Given
        ConceptMatchingService service = new ConceptMatchingService(
            match -> autoCreateDecision(match.selectedTerm())
        );

        // When / Then
        assertThatThrownBy(() -> service.decideMatches(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("matches must not be null");
    }

    @Test
    void rejectsDecisionRequest_whenPolicyDoesNotReturnMatchingTerm() {
        // Given
        ConceptMatchingService service = new ConceptMatchingService(
            match -> autoCreateDecision(new SelectedTerm(RequirementElement.ACTION, "must refund"))
        );
        CandidateConceptMatchSet matchSet = new CandidateConceptMatchSet(List.of(
            noMatch(new SelectedTerm(RequirementElement.SUBJECT, "billing service"))
        ));

        // When / Then
        assertThatThrownBy(() -> service.decideMatches(matchSet))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("matching policy returned a decision for a different selected term");
    }

    @Test
    void rejectsServiceConfiguration_whenPolicyIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new ConceptMatchingService(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("matchingPolicy must not be null");
    }

    private CandidateConceptMatch noMatch(SelectedTerm selectedTerm) {
        return new CandidateConceptMatch(selectedTerm, List.of());
    }

    private ConceptMatchDecision autoCreateDecision(SelectedTerm selectedTerm) {
        return new ConceptMatchDecision(
            selectedTerm,
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            List.of(new NewConceptProposal(selectedTerm.text(), selectedTerm.requirementElement())),
            "No existing candidates found"
        );
    }
}
