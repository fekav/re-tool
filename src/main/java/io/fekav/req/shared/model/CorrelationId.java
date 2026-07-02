package io.fekav.req.shared.model;

import java.util.Objects;
import java.util.UUID;

public record CorrelationId(UUID value) {

    public CorrelationId {
        Objects.requireNonNull(value, "correlation id value must not be null");
    }

    public static CorrelationId create() {
        return new CorrelationId(UUID.randomUUID());
    }
}
