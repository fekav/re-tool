package io.fekav.req.resolution.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;

public class EvidenceBasedNodeRetrievalPolicy implements NodeRetrievalPolicy {

    private final List<NodeRetrievalPolicy> retrievalPolicies;

    public EvidenceBasedNodeRetrievalPolicy(
        List<NodeRetrievalPolicy> retrievalPolicies
    ) {
        this.retrievalPolicies = List.copyOf(
            Objects.requireNonNull(
                retrievalPolicies,
                "retrievalPolicies must not be null"
            )
        );
    }

    @Override
    public CandidateNodeMatch retrieveCandidates(RequirementElement selectedTerm) {
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");

        Map<String, RetrievedCandidateNode> candidatesByKey = new LinkedHashMap<>();
        for (NodeRetrievalPolicy retrievalPolicy : retrievalPolicies) {
            CandidateNodeMatch policyMatch =
                retrievalPolicy.retrieveCandidates(selectedTerm);
            for (RetrievedCandidateNode candidate : policyMatch.candidates()) {
                candidatesByKey.putIfAbsent(
                    candidate.candidate().candidateKey(),
                    candidate
                );
            }
        }
        return new CandidateNodeMatch(
            selectedTerm,
            List.copyOf(candidatesByKey.values())
        );
    }
}
