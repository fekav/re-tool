package io.fekav.platform.messaging;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
    EventId eventId( );
}
