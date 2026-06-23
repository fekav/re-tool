package io.fekav.platform.messaging;

import java.util.List;

public interface EventPublisher {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);

}
