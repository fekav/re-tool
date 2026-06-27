package io.fekav.req.shared.model;

import java.util.UUID;

public record ElementId(UUID value) {

    public static ElementId create() {
        return new ElementId(UUID.randomUUID());
    }
}
