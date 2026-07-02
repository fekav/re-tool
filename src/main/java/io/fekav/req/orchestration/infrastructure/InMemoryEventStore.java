package io.fekav.req.orchestration.infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.orchestration.application.EventStore;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InMemoryEventStore implements EventStore {

    private final Map<CorrelationId, List<ApplicationEvent>> eventsByCorrelationId =
        new HashMap<>();
    private final Map<CorrelationId, Set<EventId>> eventIdsByCorrelationId =
        new HashMap<>();

    @Override
    public synchronized boolean appendIfAbsent(ApplicationEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        CorrelationId correlationId = Objects.requireNonNull(
            event.correlationId(),
            "event correlationId must not be null"
        );
        EventId eventId = Objects.requireNonNull(event.eventId(), "eventId must not be null");

        Set<EventId> eventIds = eventIdsByCorrelationId.computeIfAbsent(
            correlationId,
            ignored -> new HashSet<>()
        );

        if (eventIds.contains(eventId)) {
            return false;
        }

        eventsByCorrelationId.computeIfAbsent(correlationId, ignored -> new ArrayList<>())
            .add(event);
        eventIds.add(eventId);
        return true;
    }

    @Override
    public synchronized List<ApplicationEvent> load(CorrelationId correlationId) {
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        return List.copyOf(eventsByCorrelationId.getOrDefault(correlationId, List.of()));
    }
}
