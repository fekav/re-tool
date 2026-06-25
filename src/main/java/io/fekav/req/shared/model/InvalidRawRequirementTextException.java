package io.fekav.req.shared.model;

public final class InvalidRawRequirementTextException extends RuntimeException {

    public InvalidRawRequirementTextException() {
        super("raw requirement text must not be blank");
    }
}
