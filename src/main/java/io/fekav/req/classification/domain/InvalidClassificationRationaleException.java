package io.fekav.req.classification.domain;

public final class InvalidClassificationRationaleException extends RuntimeException {

    public InvalidClassificationRationaleException() {
        super("classification rationale must not be blank");
    }
}
