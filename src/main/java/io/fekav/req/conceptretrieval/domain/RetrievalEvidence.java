package io.fekav.req.conceptretrieval.domain;

public record RetrievalEvidence(
    String policyName,
    String evidenceText,
    Double score
) {

    public RetrievalEvidence {
        if (policyName == null || policyName.isBlank()) {
            throw new IllegalArgumentException(
                "retrieval evidence policy name must not be blank"
            );
        }
        if (evidenceText == null || evidenceText.isBlank()) {
            throw new IllegalArgumentException(
                "retrieval evidence text must not be blank"
            );
        }
        if (score != null && !Double.isFinite(score)) {
            throw new IllegalArgumentException(
                "retrieval evidence score must be finite"
            );
        }

        policyName = policyName.strip();
        evidenceText = evidenceText.strip();
    }
}
