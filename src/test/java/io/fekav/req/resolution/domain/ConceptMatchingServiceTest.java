package io.fekav.req.resolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.ConceptMatchDecision;
import io.fekav.req.shared.model.ConceptMatchDecisionStatus;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RequirementElement;

class ConceptMatchingServiceTest {

    @Test
    void returnsDecisionForCandidateMatch() {
        // Given
        List<CandidateConceptMatch> handledMatches = new ArrayList<>();
        ConceptMatchingPolicy policy = match -> {
            handledMatches.add(match);
            return autoCreateDecision(match.requirementElement());
        };
        ConceptMatchingService service = new ConceptMatchingService(policy);
        CandidateConceptMatch match =
            noMatch(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));

        // When
        ConceptMatchDecision result = service.evaluateMatch(match);

        // Then
        assertThat(handledMatches).containsExactly(match);
        assertThat(result.requirementElement())
            .isEqualTo(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));
    }

    @Test
    void rejectsDecisionRequest_whenMatchIsNull() {
        // Given
        ConceptMatchingService service = new ConceptMatchingService(
            match -> autoCreateDecision(match.requirementElement())
        );

        // When / Then
        assertThatThrownBy(() -> service.evaluateMatch(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("match must not be null");
    }

    @Test
    void rejectsDecisionRequest_whenPolicyDoesNotReturnMatchingTerm() {
        // Given
        ConceptMatchingService service = new ConceptMatchingService(
            match -> autoCreateDecision(new RequirementElement(RequirementElementType.ACTION, "must refund"))
        );
        CandidateConceptMatch match =
            noMatch(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));

        // When / Then
        assertThatThrownBy(() -> service.evaluateMatch(match))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("matching policy returned a decision for a different requirement element");
    }

    @Test
    void rejectsServiceConfiguration_whenPolicyIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new ConceptMatchingService(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("matchingPolicy must not be null");
    }

    private CandidateConceptMatch noMatch(RequirementElement selectedTerm) {
        return new CandidateConceptMatch(selectedTerm, List.of());
    }

    private ConceptMatchDecision autoCreateDecision(RequirementElement selectedTerm) {
        return new ConceptMatchDecision(
            selectedTerm,
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        );
    }
}
