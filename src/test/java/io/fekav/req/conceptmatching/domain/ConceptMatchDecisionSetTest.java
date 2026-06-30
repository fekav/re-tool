package io.fekav.req.conceptmatching.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.SelectedTerm;

class ConceptMatchDecisionSetTest {

    @Test
    void trimsDecisionStrings_whenDecisionSetIsCreated() {
        // Given
        RetrievedCandidateConcept candidate = candidate("concept-1", 1.0);

        // When
        ConceptMatchDecisionSet decisionSet = new ConceptMatchDecisionSet(List.of(
            new ConceptMatchDecision(
                new SelectedTerm(" SUBJECT ", " billing service "),
                ConceptMatchDecisionStatus.AUTO_MAP_EXISTING,
                List.of(candidate),
                List.of(),
                " unique exact candidate "
            )
        ));

        // Then
        ConceptMatchDecision decision = decisionSet.decisions().getFirst();
        assertThat(decision.selectedTerm())
            .isEqualTo(new SelectedTerm("SUBJECT", "billing service"));
        assertThat(decision.rationale()).isEqualTo("unique exact candidate");
    }

    @Test
    void defensivelyCopiesCollections_whenDecisionSetIsCreated() {
        // Given
        List<ConceptMatchDecision> decisions = new ArrayList<>();
        decisions.add(new ConceptMatchDecision(
            new SelectedTerm("SUBJECT", "billing service"),
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            List.of(new NewConceptProposal("billing service", "SUBJECT")),
            "No existing candidates found"
        ));

        // When
        ConceptMatchDecisionSet decisionSet = new ConceptMatchDecisionSet(decisions);
        decisions.clear();

        // Then
        assertThat(decisionSet.decisions())
            .singleElement()
            .satisfies(decision ->
                assertThat(decision.selectedTerm())
                    .isEqualTo(new SelectedTerm("SUBJECT", "billing service"))
            );
        assertThatThrownBy(() -> decisionSet.decisions().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsDecisionSet_whenDecisionsAreEmpty() {
        // Given / When / Then
        assertThatThrownBy(() -> new ConceptMatchDecisionSet(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("concept match decision set must not be empty");
    }

    @Test
    void rejectsDecisionSet_whenDecisionIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new ConceptMatchDecisionSet(listWithNullDecision()))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("concept match decision set must not contain null");
    }

    @Test
    void rejectsDecisionSet_whenSelectedTermAppearsMoreThanOnce() {
        // Given
        ConceptMatchDecision firstDecision = autoCreateDecision(
            new SelectedTerm("SUBJECT", "billing service")
        );
        ConceptMatchDecision secondDecision = autoCreateDecision(
            new SelectedTerm("SUBJECT", "billing service")
        );

        // When / Then
        assertThatThrownBy(() -> new ConceptMatchDecisionSet(List.of(
            firstDecision,
            secondDecision
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("concept match decision set must contain one decision per selected term");
    }

    @Test
    void rejectsDecision_whenRationaleIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new ConceptMatchDecision(
            new SelectedTerm("SUBJECT", "billing service"),
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            List.of(new NewConceptProposal("billing service", "SUBJECT")),
            " "
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("concept match decision rationale must not be blank");
    }

    @Test
    void rejectsDecision_whenCandidatesContainNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new ConceptMatchDecision(
            new SelectedTerm("SUBJECT", "billing service"),
            ConceptMatchDecisionStatus.AUTO_MAP_EXISTING,
            candidateListWithNull(),
            List.of(),
            "unique exact candidate"
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("concept match decision candidates must not contain null");
    }

    @Test
    void rejectsDecision_whenNewConceptsContainNull() {
        // Given / When / Then
        assertThatThrownBy(() -> new ConceptMatchDecision(
            new SelectedTerm("SUBJECT", "billing service"),
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            newConceptListWithNull(),
            "No existing candidates found"
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("concept match decision new concepts must not contain null");
    }

    @Test
    void rejectsDecision_whenAutoMapHasNoCandidate() {
        // Given / When / Then
        assertThatThrownBy(() -> new ConceptMatchDecision(
            new SelectedTerm("SUBJECT", "billing service"),
            ConceptMatchDecisionStatus.AUTO_MAP_EXISTING,
            List.of(),
            List.of(),
            "unique exact candidate"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("auto-map decisions must contain exactly one candidate");
    }

    @Test
    void rejectsDecision_whenProposeExistingHasMultipleCandidates() {
        // Given / When / Then
        assertThatThrownBy(() -> new ConceptMatchDecision(
            new SelectedTerm("SUBJECT", "billing service"),
            ConceptMatchDecisionStatus.PROPOSE_EXISTING,
            List.of(
                candidate("concept-1", 0.8),
                candidate("concept-2", 0.7)
            ),
            List.of(),
            "best existing candidate"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("propose-existing decisions must contain exactly one candidate");
    }

    @Test
    void rejectsDecision_whenReviewRequiredHasSingleCandidate() {
        // Given / When / Then
        assertThatThrownBy(() -> new ConceptMatchDecision(
            new SelectedTerm("SUBJECT", "billing service"),
            ConceptMatchDecisionStatus.REVIEW_REQUIRED,
            List.of(candidate("concept-1", 1.0)),
            List.of(),
            "ambiguous top candidates"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("review-required decisions must contain at least two candidates");
    }

    @Test
    void rejectsDecision_whenAutoCreateHasCandidate() {
        // Given / When / Then
        assertThatThrownBy(() -> new ConceptMatchDecision(
            new SelectedTerm("SUBJECT", "billing service"),
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(candidate("concept-1", 1.0)),
            List.of(new NewConceptProposal("billing service", "SUBJECT")),
            "No existing candidates found"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("auto-create-new decisions must not contain candidates");
    }

    @Test
    void rejectsDecision_whenAutoCreateHasNoNewConcept() {
        // Given / When / Then
        assertThatThrownBy(() -> new ConceptMatchDecision(
            new SelectedTerm("SUBJECT", "billing service"),
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            List.of(),
            "No existing candidates found"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("auto-create-new decisions must contain exactly one new concept");
    }

    @Test
    void rejectsNewConceptProposal_whenLabelIsBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new NewConceptProposal(" ", "SUBJECT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("new concept proposal label must not be blank");
    }

    @Test
    void trimsNewConceptProposalStrings_whenCreated() {
        // When
        NewConceptProposal proposal = new NewConceptProposal(
            " billing service ",
            " SystemComponent "
        );

        // Then
        assertThat(proposal.label()).isEqualTo("billing service");
        assertThat(proposal.conceptType()).isEqualTo("SystemComponent");
    }

    private ConceptMatchDecision autoCreateDecision(SelectedTerm selectedTerm) {
        return new ConceptMatchDecision(
            selectedTerm,
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            List.of(new NewConceptProposal(selectedTerm.text(), selectedTerm.syntaxRole())),
            "No existing candidates found"
        );
    }

    private RetrievedCandidateConcept candidate(String candidateKey, double score) {
        return new RetrievedCandidateConcept(
            new CandidateConcept(candidateKey, "Billing Service", "SystemComponent"),
            List.of(new RetrievalEvidence("conceptName", "matched concept name", score))
        );
    }

    private List<ConceptMatchDecision> listWithNullDecision() {
        List<ConceptMatchDecision> decisions = new ArrayList<>();
        decisions.add(null);
        return decisions;
    }

    private List<RetrievedCandidateConcept> candidateListWithNull() {
        List<RetrievedCandidateConcept> candidates = new ArrayList<>();
        candidates.add(null);
        return candidates;
    }

    private List<NewConceptProposal> newConceptListWithNull() {
        List<NewConceptProposal> newConcepts = new ArrayList<>();
        newConcepts.add(null);
        return newConcepts;
    }
}
