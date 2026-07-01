package io.fekav.platform.messaging;

import java.time.Instant;

public interface ApplicationEvent {
    Instant occurredAt();

    EventId eventId();
}
