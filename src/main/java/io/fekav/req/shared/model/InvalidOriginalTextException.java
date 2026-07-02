package io.fekav.req.shared.model;

public final class InvalidOriginalTextException extends RuntimeException {

    public InvalidOriginalTextException() {
        super("original text must not be blank");
    }
}
