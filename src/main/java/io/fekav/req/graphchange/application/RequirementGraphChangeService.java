package io.fekav.req.graphchange.application;

import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.shared.event.RequirementAnalysisCompletedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RequirementGraphChangeService {

    private final CompletedAnalysisGraphChangeFactory graphChangeFactory;
    private final RequirementGraphChangePort graphChangePort;
    private final EventPublisher eventPublisher;
    private final boolean enabled;

    @Inject
    public RequirementGraphChangeService(
        CompletedAnalysisGraphChangeFactory graphChangeFactory,
        RequirementGraphChangePort graphChangePort,
        EventPublisher eventPublisher,
        @ConfigProperty(name = "req.graphchange.enabled", defaultValue = "false")
        boolean enabled
    ) {
        this.graphChangeFactory = graphChangeFactory;
        this.graphChangePort = graphChangePort;
        this.eventPublisher = eventPublisher;
        this.enabled = enabled;
    }

    public void onRequirementAnalysisCompleted(
        @Observes RequirementAnalysisCompletedEvent event
    ) {
        if (!enabled) {
            return;
        }

        PersistRequirementGraphChange graphChange = graphChangeFactory.from(event);
        RequirementGraphChangeResult result = graphChangePort.persist(graphChange);
        result.publishFollowUp(event.correlationId(), eventPublisher);
    }
}
