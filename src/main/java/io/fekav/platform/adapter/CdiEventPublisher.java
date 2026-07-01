package io.fekav.platform.adapter;

import java.util.List;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.platform.observability.Observability;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CdiEventPublisher implements EventPublisher {

    private static final Logger log = Logger.getLogger(CdiEventPublisher.class);

    @Inject
    Event<DomainEvent> domainEvent;

    @Inject
    Event<ApplicationEvent> applicationEvent;

    @ConfigProperty(name = "observability.log.domain-events", defaultValue = "true")
    boolean logDomainEvents;

    @ConfigProperty(name = "observability.log.application-events", defaultValue = "true")
    boolean logApplicationEvents;

    @Override
    public void publish(DomainEvent event) {
        long startNanos = System.nanoTime();

        if (logDomainEvents) {
            log.info(
                Observability.block(
                    "domain_event.publish.start",
                    Observability.kv("event_type", event.getClass().getSimpleName()) + " " +
                        Observability.kv("event_id", event.eventId().value()) + " " +
                        Observability.kv("occurred_at", event.occurredAt()),
                    Observability.section("event_payload", event)
                )
            );
        }

        try {
            domainEvent.fire(event);

            log.info(
                Observability.event("domain_event.publish.end") + " " +
                    Observability.kv("event_type", event.getClass().getSimpleName()) + " " +
                    Observability.kv("event_id", event.eventId().value()) + " " +
                    Observability.kv("status", "ok") + " " +
                    Observability.kv("duration_ms", Observability.durationMs(startNanos))
            );
        } catch (RuntimeException | Error e) {
            log.error(
                Observability.event("domain_event.publish.end") + " " +
                    Observability.kv("event_type", event.getClass().getSimpleName()) + " " +
                    Observability.kv("event_id", event.eventId().value()) + " " +
                    Observability.kv("status", "error") + " " +
                    Observability.kv("duration_ms", Observability.durationMs(startNanos)) + " " +
                    Observability.kv("error_type", e.getClass().getSimpleName()) + " " +
                    Observability.kv("error_message", e.getMessage()),
                e
            );
            throw e;
        }
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        if (logDomainEvents) {
            log.info(
                Observability.event("domain_event.publish_all") + " " +
                    Observability.kv("event_count", events.size())
            );
        }
        events.forEach(this::publish);
    }

    @Override
    public void publish(ApplicationEvent event) {
        long startNanos = System.nanoTime();

        if (logApplicationEvents) {
            log.info(
                Observability.block(
                    "application_event.publish.start",
                    Observability.kv("event_type", event.getClass().getSimpleName()) + " " +
                        Observability.kv("event_id", event.eventId().value()) + " " +
                        Observability.kv("occurred_at", event.occurredAt()),
                    Observability.section("event_payload", event)
                )
            );
        }

        try {
            applicationEvent.fire(event);

            log.info(
                Observability.event("application_event.publish.end") + " " +
                    Observability.kv("event_type", event.getClass().getSimpleName()) + " " +
                    Observability.kv("event_id", event.eventId().value()) + " " +
                    Observability.kv("status", "ok") + " " +
                    Observability.kv("duration_ms", Observability.durationMs(startNanos))
            );
        } catch (RuntimeException | Error e) {
            log.error(
                Observability.event("application_event.publish.end") + " " +
                    Observability.kv("event_type", event.getClass().getSimpleName()) + " " +
                    Observability.kv("event_id", event.eventId().value()) + " " +
                    Observability.kv("status", "error") + " " +
                    Observability.kv("duration_ms", Observability.durationMs(startNanos)) + " " +
                    Observability.kv("error_type", e.getClass().getSimpleName()) + " " +
                    Observability.kv("error_message", e.getMessage()),
                e
            );
            throw e;
        }
    }

    @Override
    public void publishApplicationEvents(List<ApplicationEvent> events) {
        if (logApplicationEvents) {
            log.info(
                Observability.event("application_event.publish_all") + " " +
                    Observability.kv("event_count", events.size())
            );
        }
        events.forEach(this::publish);
    }

}
