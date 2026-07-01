package io.fekav.req.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CandidateConceptMatchTest {

    @Test
    void trimsDomainStrings_whenValuesAreCreated() {
        // Given
        RetrievalEvidence evidence = new RetrievalEvidence(
            " conceptName ",
            " matched concept name ",
            1.0
        );

        // When
        CandidateConceptMatch match = new CandidateConceptMatch(
            new SelectedTerm(RequirementElement.SUBJECT, " billing service "),
            List.of(new RetrievedCandidateConcept(
                new CandidateConcept(
                    " concept-1 ",
                    " Billing Service ",
                    " SystemComponent "
                ),
                List.of(evidence)
            ))
        );

        // Then
        RetrievedCandidateConcept retrievedCandidate = match.candidates().getFirst();
        assertThat(match.selectedTerm())
            .isEqualTo(new SelectedTerm(RequirementElement.SUBJECT, "billing service"));
        assertThat(retrievedCandidate.candidate())
            .isEqualTo(new CandidateConcept("concept-1", "Billing Service", "SystemComponent"));
        assertThat(retrievedCandidate.evidence().getFirst().policyName())
            .isEqualTo("conceptName");
        assertThat(retrievedCandidate.evidence().getFirst().evidenceText())
            .isEqualTo("matched concept name");
    }

    @Test
    void supportsExactlyV1RequirementElements() {
        assertThat(RequirementElement.values())
            .containsExactly(
                RequirementElement.SUBJECT,
                RequirementElement.ACTION,
                RequirementElement.OBJECT,
                RequirementElement.CONDITION,
                RequirementElement.CONSTRAINT
            );
    }

    @Test
    void rejectsSelectedTerm_whenRequirementElementIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new SelectedTerm(null, "billing service"))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("selected term requirement element must not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void rejectsSelectedTerm_whenTextIsBlank(String text) {
        // Given / When / Then
        assertThatThrownBy(() -> new SelectedTerm(RequirementElement.SUBJECT, text))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("selected term text must not be blank");
    }

    @Test
    void rejectsCandidateConcept_whenCandidateKeyIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new CandidateConcept(" ", "billing service", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate concept key must not be blank");
    }

    @Test
    void rejectsCandidateConcept_whenLabelIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new CandidateConcept("concept-1", " ", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate concept label must not be blank");
    }

    @Test
    void rejectsRetrievedCandidateConcept_whenEvidenceIsEmpty() {
        // Given
        CandidateConcept candidate = new CandidateConcept("concept-1", "billing service", null);

        // When / Then
        assertThatThrownBy(() -> new RetrievedCandidateConcept(candidate, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("retrieved candidate evidence must not be empty");
    }

    @Test
    void acceptsCandidateConceptMatch_whenCandidatesAreEmpty() {
        // Given
        SelectedTerm selectedTerm = new SelectedTerm(RequirementElement.SUBJECT, "billing service");

        // When
        CandidateConceptMatch match = new CandidateConceptMatch(
            selectedTerm,
            List.of()
        );

        // Then
        assertThat(match.selectedTerm()).isEqualTo(selectedTerm);
        assertThat(match.candidates()).isEmpty();
    }

    @Test
    void defensivelyCopiesCollections_whenMatchIsCreated() {
        // Given
        List<RetrievedCandidateConcept> candidates = new ArrayList<>();
        candidates.add(candidate("concept-1"));

        // When
        CandidateConceptMatch match = new CandidateConceptMatch(
            new SelectedTerm(RequirementElement.SUBJECT, "billing service"),
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
    void rejectsCandidateConceptMatch_whenCandidatesContainNull() {
        // Given
        List<RetrievedCandidateConcept> candidates = new ArrayList<>();
        candidates.add(null);

        // When / Then
        assertThatThrownBy(() -> new CandidateConceptMatch(
            new SelectedTerm(RequirementElement.SUBJECT, "billing service"),
            candidates
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("candidates must not contain null");
    }

    private RetrievedCandidateConcept candidate(String candidateKey) {
        return new RetrievedCandidateConcept(
            new CandidateConcept(candidateKey, "Billing Service", "SystemComponent"),
            List.of(new RetrievalEvidence("conceptName", "matched concept name", 1.0))
        );
    }
}
