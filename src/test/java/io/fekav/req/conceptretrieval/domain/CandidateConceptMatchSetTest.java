package io.fekav.req.conceptretrieval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CandidateConceptMatchSetTest {

    @Test
    void trimsDomainStrings_whenValuesAreCreated() {
        // Given
        RetrievalEvidence evidence = new RetrievalEvidence(
            " orderedWeighted ",
            " matched exact label ",
            1.0
        );

        // When
        CandidateConceptMatchSet matchSet = new CandidateConceptMatchSet(List.of(
            new CandidateConceptMatch(
                new SelectedTerm(" SUBJECT ", " billing service "),
                List.of(new RetrievedCandidateConcept(
                    new CandidateConcept(
                        " concept-1 ",
                        " Billing Service ",
                        " SystemComponent "
                    ),
                    List.of(evidence)
                ))
            )
        ));

        // Then
        CandidateConceptMatch match = matchSet.matches().getFirst();
        RetrievedCandidateConcept retrievedCandidate = match.candidates().getFirst();
        assertThat(match.selectedTerm()).isEqualTo(new SelectedTerm("SUBJECT", "billing service"));
        assertThat(retrievedCandidate.candidate())
            .isEqualTo(new CandidateConcept("concept-1", "Billing Service", "SystemComponent"));
        assertThat(retrievedCandidate.evidence().getFirst().policyName())
            .isEqualTo("orderedWeighted");
        assertThat(retrievedCandidate.evidence().getFirst().evidenceText())
            .isEqualTo("matched exact label");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void rejectsSelectedTerm_whenSyntaxRoleIsBlank(String syntaxRole) {
        // Given / When / Then
        assertThatThrownBy(() -> new SelectedTerm(syntaxRole, "billing service"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("selected term syntax role must not be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void rejectsSelectedTerm_whenTextIsBlank(String text) {
        // Given / When / Then
        assertThatThrownBy(() -> new SelectedTerm("SUBJECT", text))
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
    void rejectsLookupHit_whenEvidenceTextIsBlank() {
        // Given
        CandidateConcept candidate = new CandidateConcept("concept-1", "billing service", null);

        // When / Then
        assertThatThrownBy(() -> new CandidateLookupHit(
            candidate,
            "exactLabel",
            " "
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate lookup hit evidence text must not be blank");
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
        SelectedTerm selectedTerm = new SelectedTerm("SUBJECT", "billing service");

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
    void defensivelyCopiesCollections_whenMatchSetIsCreated() {
        // Given
        List<CandidateConceptMatch> matches = new ArrayList<>();
        matches.add(new CandidateConceptMatch(
            new SelectedTerm("SUBJECT", "billing service"),
            List.of()
        ));

        // When
        CandidateConceptMatchSet matchSet = new CandidateConceptMatchSet(matches);
        matches.clear();

        // Then
        assertThat(matchSet.matches())
            .singleElement()
            .satisfies(match ->
                assertThat(match.selectedTerm())
                    .isEqualTo(new SelectedTerm("SUBJECT", "billing service"))
            );
        assertThatThrownBy(() -> matchSet.matches().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsMatchSet_whenSelectedTermAppearsMoreThanOnce() {
        // Given
        SelectedTerm selectedTerm = new SelectedTerm("SUBJECT", "billing service");
        CandidateConceptMatch firstMatch = new CandidateConceptMatch(
            selectedTerm,
            List.of()
        );
        CandidateConceptMatch secondMatch = new CandidateConceptMatch(
            selectedTerm,
            List.of()
        );

        // When / Then
        assertThatThrownBy(() -> new CandidateConceptMatchSet(List.of(firstMatch, secondMatch)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate concept match set must contain one match per selected term");
    }
}
