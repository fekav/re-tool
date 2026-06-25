package io.fekav.req.classification.domain;

public final class InvalidConfidenceScoreException extends RuntimeException {

    public InvalidConfidenceScoreException() {
        super("confidence score must be between 0.0 and 1.0");
    }
}
