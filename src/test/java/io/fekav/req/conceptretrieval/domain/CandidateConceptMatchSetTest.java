package io.fekav.req.conceptretrieval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CandidateConceptMatchSetTest {

    @Test
    void trimsValues_whenDomainObjectsAreCreated() {
        // When
        CandidateConceptMatch match = new CandidateConceptMatch(
            " SUBJECT ",
            " reporting dashboard ",
            List.of(),
            List.of(new RetrievalEvidence("exactMatch", "No match", null))
        );
        CandidateConcept concept = new CandidateConcept(
            " concept-1 ",
            " Reporting Dashboard ",
            " SystemComponent "
        );
        RetrievalEvidence evidence = new RetrievalEvidence(
            " exactMatch ",
            " label = reporting dashboard ",
            1.0
        );

        // Then
        assertThat(match.syntaxRole()).isEqualTo("SUBJECT");
        assertThat(match.text()).isEqualTo("reporting dashboard");
        assertThat(concept.conceptId()).isEqualTo("concept-1");
        assertThat(concept.label()).isEqualTo("Reporting Dashboard");
        assertThat(concept.conceptType()).isEqualTo("SystemComponent");
        assertThat(evidence.policyName()).isEqualTo("exactMatch");
        assertThat(evidence.evidenceText()).isEqualTo("label = reporting dashboard");
        assertThat(evidence.score()).isEqualTo(1.0);
    }

    @Test
    void acceptsNoMatchEvidence_whenCandidateListIsEmpty() {
        // Given
        RetrievalEvidence evidence = new RetrievalEvidence(
            "exactMatch",
            "No graph candidate matched 'must refund'",
            null
        );

        // When
        CandidateConceptMatch match = new CandidateConceptMatch(
            "ACTION",
            "must refund",
            List.of(),
            List.of(evidence)
        );

        // Then
        assertThat(match.syntaxRole()).isEqualTo("ACTION");
        assertThat(match.text()).isEqualTo("must refund");
        assertThat(match.candidates()).isEmpty();
        assertThat(match.evidence()).containsExactly(evidence);
    }

    @Test
    void copiesCollections_whenMatchAndMatchSetAreCreated() {
        // Given
        CandidateConcept concept = candidate("concept-1", "payment");
        RetrievalEvidence evidence = exactMatchEvidence("payment");
        List<CandidateConcept> candidates = new ArrayList<>(List.of(concept));
        List<RetrievalEvidence> evidenceItems = new ArrayList<>(List.of(evidence));
        CandidateConceptMatch match = new CandidateConceptMatch(
            "OBJECT",
            "payment",
            candidates,
            evidenceItems
        );
        List<CandidateConceptMatch> matches = new ArrayList<>(List.of(match));

        // When
        CandidateConceptMatchSet matchSet = new CandidateConceptMatchSet(matches);
        candidates.add(candidate("concept-2", "invoice"));
        evidenceItems.add(exactMatchEvidence("invoice"));
        matches.add(new CandidateConceptMatch(
            "SUBJECT",
            "commerce system",
            List.of(candidate("concept-3", "commerce system")),
            List.of(exactMatchEvidence("commerce system"))
        ));

        // Then
        assertThat(match.candidates()).containsExactly(concept);
        assertThat(match.evidence()).containsExactly(evidence);
        assertThat(matchSet.matches()).containsExactly(match);
        assertThatThrownBy(() -> match.candidates().add(candidate("concept-4", "order")))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> matchSet.matches().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsMatch_whenTextIsBlank(String text) {
        // When / Then
        assertThatThrownBy(() -> new CandidateConceptMatch(
            "SUBJECT",
            text,
            List.of(),
            List.of(exactMatchEvidence("payment"))
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate concept match text must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsMatch_whenSyntaxRoleIsBlank(String syntaxRole) {
        // When / Then
        assertThatThrownBy(() -> new CandidateConceptMatch(
            syntaxRole,
            "payment",
            List.of(),
            List.of(exactMatchEvidence("payment"))
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate concept match syntax role must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsCandidateConcept_whenConceptIdIsBlank(String conceptId) {
        // When / Then
        assertThatThrownBy(() -> new CandidateConcept(conceptId, "payment", "BusinessObject"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate concept id must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsCandidateConcept_whenLabelIsBlank(String label) {
        // When / Then
        assertThatThrownBy(() -> new CandidateConcept("concept-1", label, "BusinessObject"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate concept label must not be blank");
    }

    @Test
    void normalizesCandidateConceptType_whenTypeIsBlank() {
        // When
        CandidateConcept concept = new CandidateConcept("concept-1", "payment", " ");

        // Then
        assertThat(concept.conceptType()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsRetrievalEvidence_whenPolicyNameIsBlank(String policyName) {
        // When / Then
        assertThatThrownBy(() -> new RetrievalEvidence(policyName, "label = payment", 1.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("retrieval evidence policy name must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsRetrievalEvidence_whenEvidenceTextIsBlank(String evidenceText) {
        // When / Then
        assertThatThrownBy(() -> new RetrievalEvidence("exactMatch", evidenceText, 1.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("retrieval evidence text must not be blank");
    }

    @Test
    void rejectsRetrievalEvidence_whenScoreIsNotFinite() {
        // When / Then
        assertThatThrownBy(() ->
            new RetrievalEvidence("exactMatch", "label = payment", Double.NaN)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("retrieval evidence score must be finite");
    }

    @Test
    void rejectsMatch_whenEvidenceIsEmpty() {
        // When / Then
        assertThatThrownBy(() -> new CandidateConceptMatch(
            "OBJECT",
            "payment",
            List.of(),
            List.of()
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate concept match evidence must not be empty");
    }

    @Test
    void rejectsMatchSet_whenSelectedTermAppearsMoreThanOnce() {
        // Given
        CandidateConceptMatch first = match("SUBJECT", "customer");
        CandidateConceptMatch second = match("SUBJECT", "customer");

        // When / Then
        assertThatThrownBy(() -> new CandidateConceptMatchSet(List.of(first, second)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("candidate concept match set must contain one entry per selected term");
    }

    @Test
    void rejectsMatchSet_whenEntryIsNull() {
        // Given
        List<CandidateConceptMatch> matches = new ArrayList<>();
        matches.add(match("OBJECT", "payment"));
        matches.add(null);

        // When / Then
        assertThatThrownBy(() -> new CandidateConceptMatchSet(matches))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("candidate concept match must not be null");
    }

    private CandidateConceptMatch match(String syntaxRole, String text) {
        return new CandidateConceptMatch(
            syntaxRole,
            text,
            List.of(candidate("concept-1", text)),
            List.of(exactMatchEvidence(text))
        );
    }

    private CandidateConcept candidate(String id, String label) {
        return new CandidateConcept(id, label, "BusinessObject");
    }

    private RetrievalEvidence exactMatchEvidence(String label) {
        return new RetrievalEvidence("exactMatch", "label = " + label, 1.0);
    }
}
