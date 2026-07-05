package io.fekav.req.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RequirementElement;

class NodeMatchDecisionTest {

    @Test
    void trimsDecisionStrings_whenDecisionIsCreated() {
        // Given
        RetrievedCandidateNode candidate = candidate("concept-1", 1.0);

        // When
        NodeMatchDecision decision = new NodeMatchDecision(
            new RequirementElement(RequirementElementType.SUBJECT, " billing service "),
            NodeMatchDecisionStatus.AUTO_MAP_EXISTING,
            List.of(candidate),
            " unique exact candidate "
        );

        // Then
        assertThat(decision.requirementElement())
            .isEqualTo(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));
        assertThat(decision.rationale()).isEqualTo("unique exact candidate");
    }

    @Test
    void defensivelyCopiesCollections_whenDecisionIsCreated() {
        // Given
        List<RetrievedCandidateNode> candidates = new ArrayList<>();
        candidates.add(candidate("concept-1", 1.0));

        // When
        NodeMatchDecision decision = new NodeMatchDecision(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            NodeMatchDecisionStatus.AUTO_MAP_EXISTING,
            candidates,
            "unique exact candidate"
        );
        candidates.clear();

        // Then
        assertThat(decision.candidates())
            .singleElement()
            .satisfies(candidate ->
                assertThat(candidate.candidate().candidateKey()).isEqualTo("concept-1")
            );
        assertThatThrownBy(() -> decision.candidates().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsDecision_whenRationaleIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new NodeMatchDecision(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            NodeMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            " "
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("node match decision rationale must not be blank");
    }

    @Test
    void rejectsDecision_whenRequirementElementIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new NodeMatchDecision(
            null,
            NodeMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found"
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("requirementElement must not be null");
    }

    @Test
    void rejectsDecision_whenCandidatesContainNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new NodeMatchDecision(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            NodeMatchDecisionStatus.AUTO_MAP_EXISTING,
            candidateListWithNull(),
            "unique exact candidate"
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("node match decision candidates must not contain null");
    }

    @Test
    void rejectsDecision_whenAutoMapHasNoCandidate() {
        // Given / When / Then
        assertThatThrownBy(() -> new NodeMatchDecision(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            NodeMatchDecisionStatus.AUTO_MAP_EXISTING,
            List.of(),
            "unique exact candidate"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("auto-map decisions must contain exactly one candidate");
    }

    @Test
    void rejectsDecision_whenProposeExistingHasMultipleCandidates() {
        // Given / When / Then
        assertThatThrownBy(() -> new NodeMatchDecision(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            NodeMatchDecisionStatus.PROPOSE_EXISTING,
            List.of(
                candidate("concept-1", 0.8),
                candidate("concept-2", 0.7)
            ),
            "best existing candidate"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("propose-existing decisions must contain exactly one candidate");
    }

    @Test
    void rejectsDecision_whenReviewRequiredHasSingleCandidate() {
        // Given / When / Then
        assertThatThrownBy(() -> new NodeMatchDecision(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            NodeMatchDecisionStatus.REVIEW_REQUIRED,
            List.of(candidate("concept-1", 1.0)),
            "ambiguous top candidates"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("review-required decisions must contain at least two candidates");
    }

    @Test
    void rejectsDecision_whenAutoCreateHasCandidate() {
        // Given / When / Then
        assertThatThrownBy(() -> new NodeMatchDecision(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            NodeMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(candidate("concept-1", 1.0)),
            "No existing candidates found"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("auto-create-new decisions must not contain candidates");
    }

    private RetrievedCandidateNode candidate(String candidateKey, double score) {
        return new RetrievedCandidateNode(
            new CandidateNode(candidateKey, "Billing Service", NodeType.CONCEPT),
            List.of(new RetrievalEvidence("nodeName", "matched node name", score))
        );
    }

    private List<RetrievedCandidateNode> candidateListWithNull() {
        List<RetrievedCandidateNode> candidates = new ArrayList<>();
        candidates.add(null);
        return candidates;
    }
}
