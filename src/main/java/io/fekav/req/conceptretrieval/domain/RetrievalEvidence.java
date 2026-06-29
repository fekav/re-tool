package io.fekav.req.conceptretrieval.domain;

public record RetrievalEvidence(
    String policyName,
    String evidenceText,
    double score
) {

    public RetrievalEvidence {
        if (policyName == null || policyName.isBlank()) {
            throw new IllegalArgumentException("retrieval evidence policy name must not be blank");
        }
        if (evidenceText == null || evidenceText.isBlank()) {
            throw new IllegalArgumentException("retrieval evidence text must not be blank");
        }
        if (Double.isNaN(score) || score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("retrieval evidence score must be between 0.0 and 1.0");
        }

        policyName = policyName.strip();
        evidenceText = evidenceText.strip();
    }
}
