package io.fekav.req.resolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.ConceptMatchDecision;
import io.fekav.req.shared.model.ConceptMatchDecisionStatus;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RequirementElement;

class ThresholdConceptMatchingPolicyTest {

    private static final double AUTO_MAP_THRESHOLD = 0.75;
    private static final double BELOW_THRESHOLD_SCORE = 0.65;

    private final RequirementElement selectedTerm = new RequirementElement(RequirementElementType.SUBJECT, "billing service");
    private final ThresholdConceptMatchingPolicy policy =
        new ThresholdConceptMatchingPolicy(AUTO_MAP_THRESHOLD);

    @Test
    void returnsAutoMapExisting_whenUniqueCandidateReachesThreshold() {
        // Given
        CandidateConceptMatch match = new CandidateConceptMatch(
            selectedTerm,
            List.of(
                candidate("concept-1", "Billing Service", AUTO_MAP_THRESHOLD),
                candidate("concept-2", "Billing API", BELOW_THRESHOLD_SCORE)
            )
        );

        // When
        ConceptMatchDecision decision = policy.decide(match);

        // Then
        assertThat(decision.requirementElement()).isEqualTo(selectedTerm);
        assertThat(decision.status()).isEqualTo(ConceptMatchDecisionStatus.AUTO_MAP_EXISTING);
        assertThat(decision.candidates())
            .extracting(retrieved -> retrieved.candidate().candidateKey())
            .containsExactly("concept-1");
        assertThat(decision.rationale())
            .contains("auto-map threshold " + AUTO_MAP_THRESHOLD)
            .contains("score " + AUTO_MAP_THRESHOLD);
    }

    @Test
    void returnsReviewRequired_whenTopThresholdCandidatesTie() {
        // Given
        CandidateConceptMatch match = new CandidateConceptMatch(
            selectedTerm,
            List.of(
                candidate("concept-2", "Billing API", AUTO_MAP_THRESHOLD),
                candidate("concept-1", "Billing Service", AUTO_MAP_THRESHOLD),
                candidate("concept-3", "Billing Job", BELOW_THRESHOLD_SCORE)
            )
        );

        // When
        ConceptMatchDecision decision = policy.decide(match);

        // Then
        assertThat(decision.status()).isEqualTo(ConceptMatchDecisionStatus.REVIEW_REQUIRED);
        assertThat(decision.candidates())
            .extracting(retrieved -> retrieved.candidate().candidateKey())
            .containsExactly("concept-2", "concept-1");
        assertThat(decision.rationale())
            .contains("Multiple top candidates share score " + AUTO_MAP_THRESHOLD)
            .contains("auto-map threshold " + AUTO_MAP_THRESHOLD);
    }

    @Test
    void returnsProposeExisting_whenBestCandidateIsBelowThreshold() {
        // Given
        CandidateConceptMatch match = new CandidateConceptMatch(
            selectedTerm,
            List.of(
                candidate("concept-1", "Billing Service", BELOW_THRESHOLD_SCORE),
                candidate("concept-2", "Billing API", BELOW_THRESHOLD_SCORE - 0.1)
            )
        );

        // When
        ConceptMatchDecision decision = policy.decide(match);

        // Then
        assertThat(decision.status()).isEqualTo(ConceptMatchDecisionStatus.PROPOSE_EXISTING);
        assertThat(decision.candidates())
            .extracting(retrieved -> retrieved.candidate().candidateKey())
            .containsExactly("concept-1");
        assertThat(decision.rationale())
            .contains("Top candidate score " + BELOW_THRESHOLD_SCORE)
            .contains("auto-map threshold " + AUTO_MAP_THRESHOLD);
    }

    @Test
    void returnsAutoCreateNew_whenCandidatesAreEmpty() {
        // Given
        CandidateConceptMatch match = new CandidateConceptMatch(selectedTerm, List.of());

        // When
        ConceptMatchDecision decision = policy.decide(match);

        // Then
        assertThat(decision.requirementElement()).isEqualTo(selectedTerm);
        assertThat(decision.status()).isEqualTo(ConceptMatchDecisionStatus.AUTO_CREATE_NEW);
        assertThat(decision.candidates()).isEmpty();
        assertThat(decision.rationale())
            .isEqualTo("No existing candidates found; auto-creating concept from selected term.");
    }

    @Test
    void usesHighestEvidenceScorePerCandidate_whenChoosingDecision() {
        // Given
        CandidateConceptMatch match = new CandidateConceptMatch(
            selectedTerm,
            List.of(
                candidate("concept-1", "Billing Service", 0.4, 1.0),
                candidate("concept-2", "Billing API", AUTO_MAP_THRESHOLD)
            )
        );

        // When
        ConceptMatchDecision decision = policy.decide(match);

        // Then
        assertThat(decision.status()).isEqualTo(ConceptMatchDecisionStatus.AUTO_MAP_EXISTING);
        assertThat(decision.candidates())
            .extracting(retrieved -> retrieved.candidate().candidateKey())
            .containsExactly("concept-1");
    }

    @Test
    void keepsRetrievalOrder_whenBelowThresholdCandidatesTie() {
        // Given
        CandidateConceptMatch match = new CandidateConceptMatch(
            selectedTerm,
            List.of(
                candidate("concept-2", "Billing API", BELOW_THRESHOLD_SCORE),
                candidate("concept-1", "Billing Service", BELOW_THRESHOLD_SCORE)
            )
        );

        // When
        ConceptMatchDecision decision = policy.decide(match);

        // Then
        assertThat(decision.status()).isEqualTo(ConceptMatchDecisionStatus.PROPOSE_EXISTING);
        assertThat(decision.candidates())
            .extracting(retrieved -> retrieved.candidate().candidateKey())
            .containsExactly("concept-2");
    }

    @Test
    void rejectsDecision_whenMatchIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> policy.decide(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("match must not be null");
    }

    @Test
    void usesDefaultAutoMapThreshold_whenNoThresholdIsProvided() {
        // Given
        ThresholdConceptMatchingPolicy defaultPolicy = new ThresholdConceptMatchingPolicy();
        CandidateConceptMatch match = new CandidateConceptMatch(
            selectedTerm,
            List.of(candidate(
                "concept-1",
                "Billing Service",
                ThresholdConceptMatchingPolicy.DEFAULT_AUTO_MAP_THRESHOLD
            ))
        );

        // When
        ConceptMatchDecision decision = defaultPolicy.decide(match);

        // Then
        assertThat(decision.status()).isEqualTo(ConceptMatchDecisionStatus.AUTO_MAP_EXISTING);
        assertThat(decision.rationale())
            .contains(
                "auto-map threshold " +
                    ThresholdConceptMatchingPolicy.DEFAULT_AUTO_MAP_THRESHOLD
            );
    }

    @Test
    void rejectsConfiguration_whenAutoMapThresholdIsNegative() {
        // Given / When / Then
        assertThatThrownBy(() -> new ThresholdConceptMatchingPolicy(-0.01))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("auto-map threshold must be between 0.0 and 1.0");
    }

    @Test
    void rejectsConfiguration_whenAutoMapThresholdIsAboveOne() {
        // Given / When / Then
        assertThatThrownBy(() -> new ThresholdConceptMatchingPolicy(1.01))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("auto-map threshold must be between 0.0 and 1.0");
    }

    @Test
    void rejectsConfiguration_whenAutoMapThresholdIsNaN() {
        // Given / When / Then
        assertThatThrownBy(() -> new ThresholdConceptMatchingPolicy(Double.NaN))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("auto-map threshold must be between 0.0 and 1.0");
    }

    private RetrievedCandidateConcept candidate(
        String candidateKey,
        String label,
        double... scores
    ) {
        List<RetrievalEvidence> evidence = java.util.Arrays.stream(scores)
            .mapToObj(score -> new RetrievalEvidence("conceptName", "matched concept name", score))
            .toList();
        return new RetrievedCandidateConcept(
            new CandidateConcept(candidateKey, label, "SystemComponent"),
            evidence
        );
    }
}
