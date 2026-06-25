package io.fekav.platform.structuredoutput;

public final class InvalidStructuredOutputException extends RuntimeException {

    public InvalidStructuredOutputException(String message) {
        super(message);
    }

    public InvalidStructuredOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
