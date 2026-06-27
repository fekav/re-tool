package io.fekav.req.ingestion.domain;

public final class InvalidRequirementIngestionException extends RuntimeException {

    public InvalidRequirementIngestionException(String message) {
        super(message);
    }
}
