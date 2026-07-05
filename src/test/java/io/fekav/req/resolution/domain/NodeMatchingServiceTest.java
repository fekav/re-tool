package io.fekav.req.resolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RequirementElement;

class NodeMatchingServiceTest {

    @Test
    void returnsDecisionForCandidateMatch() {
        // Given
        List<CandidateNodeMatch> handledMatches = new ArrayList<>();
        NodeMatchingPolicy policy = match -> {
            handledMatches.add(match);
            return autoCreateDecision(match.requirementElement());
        };
        NodeMatchingService service = new NodeMatchingService(policy);
        CandidateNodeMatch match =
            noMatch(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));

        // When
        NodeMatchDecision result = service.evaluateMatch(match);

        // Then
        assertThat(handledMatches).containsExactly(match);
        assertThat(result.requirementElement())
            .isEqualTo(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));
    }

    @Test
    void rejectsDecisionRequest_whenMatchIsNull() {
        // Given
        NodeMatchingService service = new NodeMatchingService(
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
        NodeMatchingService service = new NodeMatchingService(
            match -> autoCreateDecision(new RequirementElement(RequirementElementType.ACTION, "must refund"))
        );
        CandidateNodeMatch match =
            noMatch(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));

        // When / Then
        assertThatThrownBy(() -> service.evaluateMatch(match))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("matching policy returned a decision for a different requirement element");
    }

    @Test
    void rejectsServiceConfiguration_whenPolicyIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new NodeMatchingService(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("matchingPolicy must not be null");
    }

    private CandidateNodeMatch noMatch(RequirementElement selectedTerm) {
        return new CandidateNodeMatch(selectedTerm, List.of());
    }

    private NodeMatchDecision autoCreateDecision(RequirementElement selectedTerm) {
        return new NodeMatchDecision(
            selectedTerm,
            NodeMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        );
    }
}
