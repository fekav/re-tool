package io.fekav.platform.messaging;

import java.util.UUID;

public record EventId(UUID value) {

    public static EventId create() {
        return new EventId(UUID.randomUUID());
    }
}
