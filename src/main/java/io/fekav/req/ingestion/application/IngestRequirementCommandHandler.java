package io.fekav.req.ingestion.application;

import java.time.Instant;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.shared.event.RequirementIngestedEvent;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.OriginalText;
import io.fekav.req.shared.model.Provenance;
import io.fekav.req.shared.model.SourceMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IngestRequirementCommandHandler
        implements CommandHandler<RequirementIngestedEvent, IngestRequirementCommand> {

    private final EventPublisher eventPublisher;

    @Inject
    public IngestRequirementCommandHandler(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public RequirementIngestedEvent handle(IngestRequirementCommand command) {
        OriginalText originalText = new OriginalText(command.originalText());
        Provenance provenance = Provenance.create(
            ElementId.create(),
            originalText,
            SourceMetadata.apiRequest(),
            Instant.now()
        );
        RequirementIngestedEvent event = RequirementIngestedEvent.create(provenance);

        eventPublisher.publish(event);

        return event;
    }

    @Override
    public Class<IngestRequirementCommand> commandType() {
        return IngestRequirementCommand.class;
    }
}
