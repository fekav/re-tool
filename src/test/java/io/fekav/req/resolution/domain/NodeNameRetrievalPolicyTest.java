package io.fekav.req.resolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RequirementElementType;
import io.fekav.req.shared.model.RequirementElement;

class NodeNameRetrievalPolicyTest {

    private final RequirementElement selectedTerm = new RequirementElement(RequirementElementType.SUBJECT, "billing service");

    @Test
    void returnsNameCandidatesWithFullScore_whenLookupMatches() {
        // Given
        List<RequirementElement> lookupTerms = new ArrayList<>();
        NodeNameRetrievalPolicy policy = new NodeNameRetrievalPolicy(
            lookup(
                lookupTerms,
                term -> List.of(
                    candidate("concept-2", "Billing API"),
                    candidate("concept-1", "Billing Service")
                )
            )
        );

        // When
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(lookupTerms).containsExactly(selectedTerm);
        assertThat(match.requirementElement()).isEqualTo(selectedTerm);
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
                    "nodeName",
                    "Matched node name 'billing service' to graph candidate 'Billing API'",
                    1.0
                ),
                tuple(
                    "concept-1",
                    "Billing Service",
                    "nodeName",
                    "Matched node name 'billing service' to graph candidate 'Billing Service'",
                    1.0
                )
            );
    }

    @Test
    void returnsEmptyCandidates_whenLookupMisses() {
        // Given
        List<RequirementElement> lookupTerms = new ArrayList<>();
        NodeNameRetrievalPolicy policy = new NodeNameRetrievalPolicy(
            lookup(lookupTerms, term -> List.of())
        );

        // When
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(lookupTerms).containsExactly(selectedTerm);
        assertThat(match.requirementElement()).isEqualTo(selectedTerm);
        assertThat(match.candidates()).isEmpty();
    }

    @Test
    void keepsFirstCandidate_whenLookupReturnsDuplicateCandidateKeys() {
        // Given
        NodeNameRetrievalPolicy policy = new NodeNameRetrievalPolicy(
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
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

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
                    "Matched node name 'billing service' to graph candidate 'Billing Service'"
                ),
                tuple(
                    "concept-2",
                    "Billing API",
                    "Matched node name 'billing service' to graph candidate 'Billing API'"
                )
            );
    }

    @Test
    void rejectsRetrieval_whenSelectedTermIsNull() {
        // Given
        NodeNameRetrievalPolicy policy = new NodeNameRetrievalPolicy(
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
        assertThatThrownBy(() -> new NodeNameRetrievalPolicy(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("candidateLookup must not be null");
    }

    private CandidateNode candidate(String candidateKey, String label) {
        return new CandidateNode(candidateKey, label, NodeType.CONCEPT);
    }

    private CandidateLookup lookup(
        List<RequirementElement> lookupTerms,
        Function<RequirementElement, List<CandidateNode>> candidates
    ) {
        return term -> {
            lookupTerms.add(term);
            return new ArrayList<>(candidates.apply(term));
        };
    }
}
