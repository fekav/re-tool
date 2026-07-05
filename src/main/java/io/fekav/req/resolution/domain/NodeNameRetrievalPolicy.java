package io.fekav.req.resolution.domain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;

public class NodeNameRetrievalPolicy implements NodeRetrievalPolicy {

    public static final String POLICY_NAME = "nodeName";
    public static final double SCORE = 1.0;

    private final CandidateLookup candidateLookup;

    public NodeNameRetrievalPolicy(CandidateLookup candidateLookup) {
        this.candidateLookup = Objects.requireNonNull(
            candidateLookup,
            "candidateLookup must not be null"
        );
    }

    @Override
    public CandidateNodeMatch retrieveCandidates(RequirementElement selectedTerm) {
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");

        List<CandidateNode> candidates = deduplicatedCandidates(
            candidateLookup.findCandidates(selectedTerm)
        );
        List<RetrievedCandidateNode> retrievedCandidates = candidates
            .stream()
            .map(candidate -> retrievedCandidate(selectedTerm, candidate))
            .toList();
        return new CandidateNodeMatch(selectedTerm, retrievedCandidates);
    }

    private RetrievedCandidateNode retrievedCandidate(
        RequirementElement selectedTerm,
        CandidateNode candidate
    ) {
        RetrievalEvidence evidence = new RetrievalEvidence(
            POLICY_NAME,
            evidenceText(selectedTerm, candidate),
            SCORE
        );
        return new RetrievedCandidateNode(candidate, List.of(evidence));
    }

    private List<CandidateNode> deduplicatedCandidates(Collection<CandidateNode> candidates) {
        Map<String, CandidateNode> candidatesByKey = new LinkedHashMap<>();
        for (CandidateNode candidate : List.copyOf(candidates)) {
            candidatesByKey.putIfAbsent(candidate.candidateKey(), candidate);
        }
        return List.copyOf(candidatesByKey.values());
    }

    private String evidenceText(RequirementElement selectedTerm, CandidateNode candidate) {
        return "Matched node name '" + selectedTerm.text() + "' to graph candidate '" +
            candidate.label() + "'";
    }
}
