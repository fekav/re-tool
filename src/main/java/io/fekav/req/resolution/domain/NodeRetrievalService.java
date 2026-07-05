package io.fekav.req.resolution.domain;

import java.util.Objects;

import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.RequirementElement;

public class NodeRetrievalService {

    private final NodeRetrievalPolicy retrievalPolicy;

    public NodeRetrievalService(NodeRetrievalPolicy retrievalPolicy) {
        this.retrievalPolicy = Objects.requireNonNull(
            retrievalPolicy,
            "retrievalPolicy must not be null"
        );
    }

    public CandidateNodeMatch retrieveCandidates(RequirementElement selectedTerm) {
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");
        CandidateNodeMatch match = retrievalPolicy.retrieveCandidates(selectedTerm);
        if (!selectedTerm.equals(match.requirementElement())) {
            throw new IllegalStateException(
                "retrieval policy returned a match for a different selected term"
            );
        }
        return match;
    }
}
