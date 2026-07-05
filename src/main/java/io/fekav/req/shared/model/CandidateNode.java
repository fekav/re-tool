package io.fekav.req.shared.model;

public record CandidateNode(
    String candidateKey,
    String label,
    NodeType nodeType
) {

    public CandidateNode {
        if (candidateKey == null || candidateKey.isBlank()) {
            throw new IllegalArgumentException("candidate node key must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("candidate node label must not be blank");
        }
        if (nodeType == null) {
            throw new NullPointerException("candidate node type must not be null");
        }

        candidateKey = candidateKey.strip();
        label = label.strip();
    }
}
