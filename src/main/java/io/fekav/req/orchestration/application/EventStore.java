package io.fekav.req.orchestration.application;

import java.util.List;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.platform.messaging.CorrelationId;

public interface EventStore {

    boolean appendIfAbsent(ApplicationEvent event);

    List<ApplicationEvent> load(CorrelationId correlationId);
}
