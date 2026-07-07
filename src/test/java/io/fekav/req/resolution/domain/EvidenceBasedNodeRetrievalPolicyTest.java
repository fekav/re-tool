package io.fekav.req.resolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.NodeType;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

class EvidenceBasedNodeRetrievalPolicyTest {

    private final RequirementElement selectedTerm =
        new RequirementElement(RequirementElementType.SUBJECT, "billing service");

    @Test
    void callsPoliciesInConfiguredOrder() {
        // Given
        List<String> policyCalls = new ArrayList<>();
        EvidenceBasedNodeRetrievalPolicy policy = new EvidenceBasedNodeRetrievalPolicy(
            List.of(
                recordingPolicy(policyCalls, "exact", List.of()),
                recordingPolicy(policyCalls, "tokenOverlap", List.of())
            )
        );

        // When
        policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(policyCalls).containsExactly("exact", "tokenOverlap");
    }

    @Test
    void combinesExactAndTokenOverlapCandidates() {
        // Given
        EvidenceBasedNodeRetrievalPolicy policy = new EvidenceBasedNodeRetrievalPolicy(
            List.of(
                policyReturning(candidate("concept-1", "Billing Service", "nodeName", 1.0)),
                policyReturning(candidate("concept-2", "Billing API", "tokenOverlap", 0.5))
            )
        );

        // When
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(match.candidates())
            .extracting(
                retrieved -> retrieved.candidate().candidateKey(),
                retrieved -> retrieved.evidence().getFirst().policyName(),
                retrieved -> retrieved.evidence().getFirst().score()
            )
            .containsExactly(
                tuple("concept-1", "nodeName", 1.0),
                tuple("concept-2", "tokenOverlap", 0.5)
            );
    }

    @Test
    void deduplicatesCandidatesByCandidateKey() {
        // Given
        EvidenceBasedNodeRetrievalPolicy policy = new EvidenceBasedNodeRetrievalPolicy(
            List.of(
                policyReturning(candidate("concept-1", "Billing Service", "nodeName", 1.0)),
                policyReturning(candidate("concept-1", "Billing Service", "tokenOverlap", 0.99))
            )
        );

        // When
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(match.candidates())
            .extracting(retrieved -> retrieved.candidate().candidateKey())
            .containsExactly("concept-1");
    }

    @Test
    void keepsExactEvidenceOnly_whenSameCandidateIsFoundByTokenOverlap() {
        // Given
        EvidenceBasedNodeRetrievalPolicy policy = new EvidenceBasedNodeRetrievalPolicy(
            List.of(
                policyReturning(candidate("concept-1", "Billing Service", "nodeName", 1.0)),
                policyReturning(candidate("concept-1", "Billing Service", "tokenOverlap", 0.99))
            )
        );

        // When
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(match.candidates())
            .singleElement()
            .satisfies(candidate ->
                assertThat(candidate.evidence())
                    .extracting(RetrievalEvidence::policyName)
                    .containsExactly("nodeName")
            );
    }

    @Test
    void returnsOriginalRequirementElement() {
        // Given
        EvidenceBasedNodeRetrievalPolicy policy = new EvidenceBasedNodeRetrievalPolicy(
            List.of(policyReturning(
                candidate("concept-1", "Billing Service", "nodeName", 1.0)
            ))
        );

        // When
        CandidateNodeMatch match = policy.retrieveCandidates(selectedTerm);

        // Then
        assertThat(match.requirementElement()).isEqualTo(selectedTerm);
    }

    private NodeRetrievalPolicy recordingPolicy(
        List<String> policyCalls,
        String policyName,
        List<RetrievedCandidateNode> candidates
    ) {
        return selectedTerm -> {
            policyCalls.add(policyName);
            return new CandidateNodeMatch(selectedTerm, candidates);
        };
    }

    private NodeRetrievalPolicy policyReturning(
        RetrievedCandidateNode... candidates
    ) {
        return selectedTerm -> new CandidateNodeMatch(
            selectedTerm,
            List.of(candidates)
        );
    }

    private RetrievedCandidateNode candidate(
        String candidateKey,
        String label,
        String policyName,
        double score
    ) {
        return new RetrievedCandidateNode(
            new CandidateNode(candidateKey, label, NodeType.CONCEPT),
            List.of(new RetrievalEvidence(policyName, "matched candidate", score))
        );
    }
}
