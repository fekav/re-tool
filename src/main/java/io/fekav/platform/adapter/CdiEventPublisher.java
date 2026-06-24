package io.fekav.platform.adapter;

import java.util.List;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@ApplicationScoped
public class CdiEventPublisher implements EventPublisher {

    @Inject
    Event<DomainEvent> domainEvent;

    @Override
    public void publish(DomainEvent event) {
        domainEvent.fire(event);        
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }

}
