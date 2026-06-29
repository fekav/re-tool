package io.fekav.req.conceptretrieval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class ConceptNameRetrievalPolicyTest {

    private final SelectedTerm selectedTerm = new SelectedTerm("SUBJECT", "billing service");

    @Test
    void returnsNameCandidatesWithFullScore_whenLookupMatches() {
        // Given
        List<SelectedTerm> lookupTerms = new ArrayList<>();
        ConceptNameRetrievalPolicy policy = new ConceptNameRetrievalPolicy(
            lookup(
                lookupTerms,
                term -> List.of(
                    candidate("concept-2", "Billing API"),
                    candidate("concept-1", "Billing Service")
                )
            )
        );

        // When
        CandidateConceptMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(lookupTerms).containsExactly(selectedTerm);
        assertThat(match.selectedTerm()).isEqualTo(selectedTerm);
        assertThat(match.candidates())
            .extracting(
                retrieved -> retrieved.candidate().candidateKey(),
                retrieved -> retrieved.candidate().label(),
                retrieved -> retrieved.evidence().getFirst().policyName(),
                retrieved -> retrieved.evidence().getFirst().evidenceText(),
                retrieved -> retrieved.evidence().getFirst().score()
            )
            .containsExactly(
                tuple(
                    "concept-2",
                    "Billing API",
                    "conceptName",
                    "Matched concept name 'billing service' to graph candidate 'Billing API'",
                    1.0
                ),
                tuple(
                    "concept-1",
                    "Billing Service",
                    "conceptName",
                    "Matched concept name 'billing service' to graph candidate 'Billing Service'",
                    1.0
                )
            );
    }

    @Test
    void returnsEmptyCandidates_whenLookupMisses() {
        // Given
        List<SelectedTerm> lookupTerms = new ArrayList<>();
        ConceptNameRetrievalPolicy policy = new ConceptNameRetrievalPolicy(
            lookup(lookupTerms, term -> List.of())
        );

        // When
        CandidateConceptMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(lookupTerms).containsExactly(selectedTerm);
        assertThat(match.selectedTerm()).isEqualTo(selectedTerm);
        assertThat(match.candidates()).isEmpty();
    }

    @Test
    void keepsFirstCandidate_whenLookupReturnsDuplicateCandidateKeys() {
        // Given
        ConceptNameRetrievalPolicy policy = new ConceptNameRetrievalPolicy(
            lookup(
                new ArrayList<>(),
                term -> List.of(
                    candidate("concept-1", "Billing Service"),
                    candidate("concept-1", "Billing Service Duplicate"),
                    candidate("concept-2", "Billing API")
                )
            )
        );

        // When
        CandidateConceptMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(match.candidates())
            .extracting(
                retrieved -> retrieved.candidate().candidateKey(),
                retrieved -> retrieved.candidate().label(),
                retrieved -> retrieved.evidence().getFirst().evidenceText()
            )
            .containsExactly(
                tuple(
                    "concept-1",
                    "Billing Service",
                    "Matched concept name 'billing service' to graph candidate 'Billing Service'"
                ),
                tuple(
                    "concept-2",
                    "Billing API",
                    "Matched concept name 'billing service' to graph candidate 'Billing API'"
                )
            );
    }

    @Test
    void rejectsRetrieval_whenSelectedTermIsNull() {
        // Given
        ConceptNameRetrievalPolicy policy = new ConceptNameRetrievalPolicy(
            lookup(new ArrayList<>(), term -> List.of())
        );

        // When / Then
        assertThatThrownBy(() -> policy.retrieveCandidates(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("selectedTerm must not be null");
    }

    @Test
    void rejectsPolicyConfiguration_whenLookupPortIsNull() {
        // When / Then
        assertThatThrownBy(() -> new ConceptNameRetrievalPolicy(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("candidateLookup must not be null");
    }

    private CandidateConcept candidate(String candidateKey, String label) {
        return new CandidateConcept(candidateKey, label, "SystemComponent");
    }

    private CandidateLookup lookup(
        List<SelectedTerm> lookupTerms,
        Function<SelectedTerm, List<CandidateConcept>> candidates
    ) {
        return term -> {
            lookupTerms.add(term);
            return new ArrayList<>(candidates.apply(term));
        };
    }
}
