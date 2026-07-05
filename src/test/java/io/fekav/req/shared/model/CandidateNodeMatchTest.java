package io.fekav.req.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CandidateNodeMatchTest {

    @Test
    void trimsDomainStrings_whenValuesAreCreated() {
        // Given
        RetrievalEvidence evidence = new RetrievalEvidence(
            " nodeName ",
            " matched node name ",
            1.0
        );

        // When
        CandidateNodeMatch match = new CandidateNodeMatch(
            new RequirementElement(RequirementElementType.SUBJECT, " billing service "),
            List.of(new RetrievedCandidateNode(
                new CandidateNode(
                    " concept-1 ",
                    " Billing Service ",
                    NodeType.CONCEPT
                ),
                List.of(evidence)
            ))
        );

        // Then
        RetrievedCandidateNode retrievedCandidate = match.candidates().getFirst();
        assertThat(match.requirementElement())
            .isEqualTo(new RequirementElement(RequirementElementType.SUBJECT, "billing service"));
        assertThat(retrievedCandidate.candidate())
            .isEqualTo(new CandidateNode("concept-1", "Billing Service", NodeType.CONCEPT));
        assertThat(retrievedCandidate.evidence().getFirst().policyName())
            .isEqualTo("nodeName");
        assertThat(retrievedCandidate.evidence().getFirst().evidenceText())
            .isEqualTo("matched node name");
    }

    @Test
    void supportsExactlyV1RequirementElements() {
        assertThat(RequirementElementType.values())
            .containsExactly(
                RequirementElementType.SUBJECT,
                RequirementElementType.ACTION,
                RequirementElementType.OBJECT,
                RequirementElementType.CONDITION,
                RequirementElementType.CONSTRAINT
            );
    }

    @Test
    void rejectsSelectedTerm_whenRequirementElementIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new RequirementElement(null, "billing service"))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("selected term requirement element must not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void rejectsSelectedTerm_whenTextIsBlank(String text) {
        // Given / When / Then
        assertThatThrownBy(() -> new RequirementElement(RequirementElementType.SUBJECT, text))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("selected term text must not be blank");
    }

    @Test
    void rejectsCandidateNode_whenCandidateKeyIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new CandidateNode(" ", "billing service", NodeType.CONCEPT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate node key must not be blank");
    }

    @Test
    void rejectsCandidateNode_whenLabelIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new CandidateNode("concept-1", " ", NodeType.CONCEPT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate node label must not be blank");
    }

    @Test
    void rejectsCandidateNode_whenNodeTypeIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new CandidateNode("concept-1", "billing service", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("candidate node type must not be null");
    }

    @Test
    void rejectsRetrievedCandidateNode_whenEvidenceIsEmpty() {
        // Given
        CandidateNode candidate =
            new CandidateNode("concept-1", "billing service", NodeType.CONCEPT);

        // When / Then
        assertThatThrownBy(() -> new RetrievedCandidateNode(candidate, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("retrieved candidate evidence must not be empty");
    }

    @Test
    void acceptsCandidateNodeMatch_whenCandidatesAreEmpty() {
        // Given
        RequirementElement selectedTerm = new RequirementElement(RequirementElementType.SUBJECT, "billing service");

        // When
        CandidateNodeMatch match = new CandidateNodeMatch(
            selectedTerm,
            List.of()
        );

        // Then
        assertThat(match.requirementElement()).isEqualTo(selectedTerm);
        assertThat(match.candidates()).isEmpty();
    }

    @Test
    void defensivelyCopiesCollections_whenMatchIsCreated() {
        // Given
        List<RetrievedCandidateNode> candidates = new ArrayList<>();
        candidates.add(candidate("concept-1"));

        // When
        CandidateNodeMatch match = new CandidateNodeMatch(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            candidates
        );
        candidates.clear();

        // Then
        assertThat(match.candidates())
            .singleElement()
            .satisfies(candidate ->
                assertThat(candidate.candidate().candidateKey()).isEqualTo("concept-1")
            );
        assertThatThrownBy(() -> match.candidates().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsCandidateNodeMatch_whenCandidatesContainNull() {
        // Given
        List<RetrievedCandidateNode> candidates = new ArrayList<>();
        candidates.add(null);

        // When / Then
        assertThatThrownBy(() -> new CandidateNodeMatch(
            new RequirementElement(RequirementElementType.SUBJECT, "billing service"),
            candidates
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("candidates must not contain null");
    }

    private RetrievedCandidateNode candidate(String candidateKey) {
        return new RetrievedCandidateNode(
            new CandidateNode(candidateKey, "Billing Service", NodeType.CONCEPT),
            List.of(new RetrievalEvidence("nodeName", "matched node name", 1.0))
        );
    }
}
