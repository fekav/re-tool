package io.fekav.req.ingestion.application;

import java.util.Objects;

import io.fekav.platform.messaging.CorrelationId;

public record IngestRequirementResult(
    CorrelationId correlationId,
    Status status,
    String message
) {

    public IngestRequirementResult {
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("ingestion result message must not be blank");
        }
        message = message.strip();
    }

    public static IngestRequirementResult recorded(CorrelationId correlationId) {
        return new IngestRequirementResult(
            correlationId,
            Status.RECORDED,
            "Requirement recorded."
        );
    }

    public static IngestRequirementResult alreadyExists(CorrelationId correlationId) {
        return new IngestRequirementResult(
            correlationId,
            Status.ALREADY_EXISTS,
            "Requirement already exists."
        );
    }

    public static IngestRequirementResult reviewRequired(CorrelationId correlationId) {
        return new IngestRequirementResult(
            correlationId,
            Status.REVIEW_REQUIRED,
            "Requirement requires review."
        );
    }

    public static IngestRequirementResult internalError(
        CorrelationId correlationId,
        String message
    ) {
        return new IngestRequirementResult(
            correlationId,
            Status.INTERNAL_ERROR,
            message
        );
    }

    public enum Status {
        RECORDED,
        ALREADY_EXISTS,
        REVIEW_REQUIRED,
        INTERNAL_ERROR
    }
}
