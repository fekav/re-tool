package io.fekav.req.orchestration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventId;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.OriginalText;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.SourceMetadata;

class InMemoryEventStoreTest {

    private final InMemoryEventStore eventStore = new InMemoryEventStore();

    @Test
    void returnsTrueAndLoadsEvent_whenEventIsAppendedFirstTime() {
        // Arrange
        RequirementIngestedEvent event = ingestedEvent();

        // Act
        boolean appended = eventStore.appendIfAbsent(event);

        // Assert
        assertThat(appended).isTrue();
        assertThat(eventStore.load(event.correlationId())).containsExactly(event);
    }

    @Test
    void returnsFalseAndKeepsSingleEvent_whenSameEventIdIsAppendedAgain() {
        // Arrange
        RequirementIngestedEvent event = ingestedEvent();
        eventStore.appendIfAbsent(event);

        // Act
        boolean appended = eventStore.appendIfAbsent(event);

        // Assert
        assertThat(appended).isFalse();
        assertThat(eventStore.load(event.correlationId())).containsExactly(event);
    }

    @Test
    void returnsImmutableCopy_whenEventsAreLoaded() {
        // Arrange
        RequirementIngestedEvent event = ingestedEvent();
        eventStore.appendIfAbsent(event);

        // Act
        List<ApplicationEvent> loadedEvents = eventStore.load(event.correlationId());

        // Assert
        assertThatThrownBy(() -> loadedEvents.add(event))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(eventStore.load(event.correlationId())).containsExactly(event);
    }

    @Test
    void returnsEmptyList_whenCorrelationIdHasNoEvents() {
        // Act / Assert
        assertThat(eventStore.load(CorrelationId.create())).isEmpty();
    }

    @Test
    void rejectsEvent_whenCorrelationIdIsNull() {
        // Arrange
        ApplicationEvent event = applicationEvent(null, EventId.create());

        // Act / Assert
        assertThatThrownBy(() -> eventStore.appendIfAbsent(event))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("event correlationId must not be null");
    }

    @Test
    void rejectsEvent_whenEventIdIsNull() {
        // Arrange
        ApplicationEvent event = applicationEvent(CorrelationId.create(), null);

        // Act / Assert
        assertThatThrownBy(() -> eventStore.appendIfAbsent(event))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("eventId must not be null");
    }

    private RequirementIngestedEvent ingestedEvent() {
        return RequirementIngestedEvent.create(Provenance.create(
            ElementId.create(),
            new OriginalText("The login form must validate credentials."),
            SourceMetadata.apiRequest(),
            Instant.parse("2026-07-02T12:00:00Z")
        ));
    }

    private ApplicationEvent applicationEvent(
        CorrelationId correlationId,
        EventId eventId
    ) {
        return new ApplicationEvent() {
            @Override
            public Instant occurredAt() {
                return Instant.parse("2026-07-02T12:00:00Z");
            }

            @Override
            public EventId eventId() {
                return eventId;
            }

            @Override
            public CorrelationId correlationId() {
                return correlationId;
            }
        };
    }
}
