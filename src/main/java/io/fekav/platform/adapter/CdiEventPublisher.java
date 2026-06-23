package io.fekav.platform.adapter;

import java.util.List;

import io.fekav.platform.messaging.DomainEvent;
import io.fekav.platform.messaging.EventPublisher;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CdiEventPublisher implements EventPublisher {

    @Override
    public void publish(DomainEvent event) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'publish'");
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'publishAll'");
    }

}
