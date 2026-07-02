package io.fekav.req.orchestration.infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.orchestration.application.EventStore;
import io.fekav.req.shared.model.CorrelationId;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InMemoryEventStore implements EventStore {

    private final Map<CorrelationId, List<ApplicationEvent>> eventsByCorrelationId =
        new HashMap<>();
    private final Map<CorrelationId, Set<EventId>> eventIdsByCorrelationId =
        new HashMap<>();

    @Override
    public synchronized boolean appendIfAbsent(
        CorrelationId correlationId,
        ApplicationEvent event
    ) {
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(event, "event must not be null");

        Set<EventId> eventIds = eventIdsByCorrelationId.computeIfAbsent(
            correlationId,
            ignored -> new HashSet<>()
        );

        if (eventIds.contains(event.eventId())) {
            return false;
        }

        eventsByCorrelationId.computeIfAbsent(correlationId, ignored -> new ArrayList<>())
            .add(event);
        eventIds.add(event.eventId());
        return true;
    }

    @Override
    public synchronized List<ApplicationEvent> load(CorrelationId correlationId) {
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        return List.copyOf(eventsByCorrelationId.getOrDefault(correlationId, List.of()));
    }
}
