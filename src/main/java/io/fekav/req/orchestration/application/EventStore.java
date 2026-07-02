package io.fekav.req.orchestration.application;

import java.util.List;

import io.fekav.platform.messaging.ApplicationEvent;
import io.fekav.req.shared.model.CorrelationId;

public interface EventStore {

    boolean appendIfAbsent(CorrelationId correlationId, ApplicationEvent event);

    List<ApplicationEvent> load(CorrelationId correlationId);
}
