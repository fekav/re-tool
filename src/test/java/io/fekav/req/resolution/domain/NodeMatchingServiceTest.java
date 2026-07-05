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
    void returnsResultForCandidateMatch() {
        // Given
        List<CandidateNodeMatch> handledMatches = new ArrayList<>();
        NodeMatchingPolicy policy = match -> {
            handledMatches.add(match);
            return NodeMatchingResult.decided(autoCreateDecision(match.requirementElement()));
        };
        NodeMatchingService service = new NodeMatchingService(policy);
        CandidateNodeMatch match =
            noMatch(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));

        // When
        NodeMatchingResult result = service.evaluateMatch(match);

        // Then
        assertThat(handledMatches).containsExactly(match);
        assertThat(result).isInstanceOf(NodeMatchingResult.Decided.class);
        assertThat(((NodeMatchingResult.Decided) result).decision().requirementElement())
            .isEqualTo(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));
    }

    @Test
    void rejectsMatchEvaluation_whenMatchIsNull() {
        // Given
        NodeMatchingService service = new NodeMatchingService(
            match -> NodeMatchingResult.decided(autoCreateDecision(match.requirementElement()))
        );

        // When / Then
        assertThatThrownBy(() -> service.evaluateMatch(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("match must not be null");
    }

    @Test
    void rejectsMatchEvaluation_whenPolicyDoesNotReturnMatchingElement() {
        // Given
        NodeMatchingService service = new NodeMatchingService(
            match -> NodeMatchingResult.decided(autoCreateDecision(
                new RequirementElement(RequirementElementType.ACTION, "must refund")
            ))
        );
        CandidateNodeMatch match =
            noMatch(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));

        // When / Then
        assertThatThrownBy(() -> service.evaluateMatch(match))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("matching policy returned a result for a different requirement element");
    }

    @Test
    void rejectsMatchEvaluation_whenPolicyReturnsNull() {
        // Given
        NodeMatchingService service = new NodeMatchingService(match -> null);
        CandidateNodeMatch match =
            noMatch(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));

        // When / Then
        assertThatThrownBy(() -> service.evaluateMatch(match))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("matching policy result must not be null");
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
