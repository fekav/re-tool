package io.fekav.req.shared.model;

import java.util.UUID;

public record RequirementId(UUID value) {

    public static RequirementId create() {
        return new RequirementId(UUID.randomUUID());
    }
}
