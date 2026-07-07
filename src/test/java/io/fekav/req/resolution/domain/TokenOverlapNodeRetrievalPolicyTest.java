package io.fekav.req.resolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

class TokenOverlapNodeRetrievalPolicyTest {

    private final RequirementElement selectedTerm =
        new RequirementElement(RequirementElementType.SUBJECT, "billing service");

    @Test
    void returnsCandidate_whenTokenOverlapScoreReachesMinimum() {
        // Given
        List<RequirementElement> lookupTerms = new ArrayList<>();
        TokenOverlapNodeRetrievalPolicy policy = policyReturning(
            lookupTerms,
            List.of(candidate("concept-1", "billing account"))
        );

        // When
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(lookupTerms).containsExactly(selectedTerm);
        assertThat(match.requirementElement()).isEqualTo(selectedTerm);
        assertThat(match.candidates())
            .extracting(
                retrieved -> retrieved.candidate().candidateKey(),
                retrieved -> retrieved.evidence().getFirst().policyName(),
                retrieved -> retrieved.evidence().getFirst().score()
            )
            .containsExactly(tuple("concept-1", "tokenOverlap", 0.5));
    }

    @Test
    void filtersCandidate_whenTokenOverlapScoreIsBelowMinimum() {
        // Given
        RequirementElement selectedTerm =
            new RequirementElement(RequirementElementType.SUBJECT, "billing service gateway");
        TokenOverlapNodeRetrievalPolicy policy = policyReturning(
            List.of(candidate("concept-1", "billing queue"))
        );

        // When
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(match.candidates()).isEmpty();
    }

    @Test
    void capsScore_whenAllQueryTokensMatch() {
        // Given
        TokenOverlapNodeRetrievalPolicy policy = policyReturning(
            List.of(candidate("concept-1", "billing service"))
        );

        // When
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(match.candidates())
            .extracting(retrieved -> retrieved.evidence().getFirst().score())
            .containsExactly(0.99);
    }

    @Test
    void tokenizesCaseInsensitiveAndSeparatesNonLettersAndNumbers() {
        // Given
        RequirementElement selectedTerm =
            new RequirementElement(RequirementElementType.SUBJECT, "Billing-Service");
        TokenOverlapNodeRetrievalPolicy policy = policyReturning(
            List.of(candidate("concept-1", "SERVICE"))
        );

        // When
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(match.candidates())
            .extracting(
                retrieved -> retrieved.candidate().candidateKey(),
                retrieved -> retrieved.evidence().getFirst().score()
            )
            .containsExactly(tuple("concept-1", 0.5));
    }

    @Test
    void sortsCandidatesByScoreThenLabelThenCandidateKey() {
        // Given
        RequirementElement selectedTerm =
            new RequirementElement(RequirementElementType.SUBJECT, "billing service gateway");
        TokenOverlapNodeRetrievalPolicy policy = policyReturning(List.of(
            candidate("concept-z", "Zeta Billing Service"),
            candidate("concept-b", "Alpha Billing Service"),
            candidate("concept-full", "Billing Service Gateway"),
            candidate("concept-a", "Alpha Billing Service"),
            candidate("concept-low", "Gateway")
        ));

        // When
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(match.candidates())
            .extracting(retrieved -> retrieved.candidate().candidateKey())
            .containsExactly(
                "concept-full",
                "concept-a",
                "concept-b",
                "concept-z"
            );
    }

    @Test
    void returnsEmptyCandidates_whenNoCompatibleCandidatesOverlapEnough() {
        // Given
        TokenOverlapNodeRetrievalPolicy policy = policyReturning(List.of(
            candidate("concept-1", "payment adapter"),
            candidate("concept-2", "refund workflow")
        ));

        // When
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(match.candidates()).isEmpty();
    }

    @Test
    void rejectsRetrieval_whenSelectedTermIsNull() {
        // Given
        TokenOverlapNodeRetrievalPolicy policy = policyReturning(List.of());

        // When / Then
        assertThatThrownBy(() -> policy.retrieveCandidates(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("selectedTerm must not be null");
    }

    @Test
    void rejectsPolicyConfiguration_whenCompatibleLookupPortIsNull() {
        // When / Then
        assertThatThrownBy(() -> new TokenOverlapNodeRetrievalPolicy(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("compatibleCandidateLookup must not be null");
    }

    private TokenOverlapNodeRetrievalPolicy policyReturning(
        List<CandidateNode> candidates
    ) {
        return policyReturning(new ArrayList<>(), candidates);
    }

    private TokenOverlapNodeRetrievalPolicy policyReturning(
        List<RequirementElement> lookupTerms,
        List<CandidateNode> candidates
    ) {
        return new TokenOverlapNodeRetrievalPolicy(term -> {
            lookupTerms.add(term);
            return new ArrayList<>(candidates);
        });
    }

    private CandidateNode candidate(String candidateKey, String label) {
        return new CandidateNode(candidateKey, label, NodeType.CONCEPT);
    }
}
