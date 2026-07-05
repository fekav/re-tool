package io.fekav.req.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class NodeMatchReviewRequestTest {

    @Test
    void trimsReviewRequestStrings_whenRequestIsCreated() {
        // Given
        RetrievedCandidateNode candidate = candidate("concept-1", 0.8);

        // When
        NodeMatchReviewRequest reviewRequest = new NodeMatchReviewRequest(
            new RequirementElement(RequirementElementType.SUBJECT, " billing service "),
            List.of(candidate),
            " needs review "
        );

        // Then
        assertThat(reviewRequest.requirementElement())
            .isEqualTo(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));
        assertThat(reviewRequest.rationale()).isEqualTo("needs review");
    }

    @Test
    void defensivelyCopiesCollections_whenRequestIsCreated() {
        // Given
        List<RetrievedCandidateNode> candidates = new ArrayList<>();
        candidates.add(candidate("concept-1", 0.8));

        // When
        NodeMatchReviewRequest reviewRequest = new NodeMatchReviewRequest(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            candidates,
            "needs review"
        );
        candidates.clear();

        // Then
        assertThat(reviewRequest.candidates())
            .singleElement()
            .satisfies(candidate ->
                assertThat(candidate.candidate().candidateKey()).isEqualTo("concept-1")
            );
        assertThatThrownBy(() -> reviewRequest.candidates().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsRequest_whenRationaleIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new NodeMatchReviewRequest(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            List.of(candidate("concept-1", 0.8)),
            " "
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("node match review rationale must not be blank");
    }

    @Test
    void rejectsRequest_whenRequirementElementIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new NodeMatchReviewRequest(
            null,
            List.of(candidate("concept-1", 0.8)),
            "needs review"
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("requirementElement must not be null");
    }

    @Test
    void rejectsRequest_whenCandidatesContainNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new NodeMatchReviewRequest(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            candidateListWithNull(),
            "needs review"
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("node match review candidates must not contain null");
    }

    @Test
    void rejectsRequest_whenCandidatesAreEmpty() {
        // Given / When / Then
        assertThatThrownBy(() -> new NodeMatchReviewRequest(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            List.of(),
            "needs review"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("node match review requests must contain at least one candidate");
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
