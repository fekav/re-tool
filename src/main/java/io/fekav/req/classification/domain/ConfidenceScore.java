package io.fekav.req.classification.domain;

public record ConfidenceScore(double value) {

    public ConfidenceScore {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new InvalidConfidenceScoreException();
        }
    }
}
