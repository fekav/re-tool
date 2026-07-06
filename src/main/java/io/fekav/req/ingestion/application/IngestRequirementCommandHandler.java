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
        implements CommandHandler<IngestRequirementResult, IngestRequirementCommand> {

    private final EventPublisher eventPublisher;
    private final IngestionOutcomeStore outcomeStore;

    @Inject
    public IngestRequirementCommandHandler(
        EventPublisher eventPublisher,
        IngestionOutcomeStore outcomeStore
    ) {
        this.eventPublisher = eventPublisher;
        this.outcomeStore = outcomeStore;
    }

    @Override
    @Transactional
    public IngestRequirementResult handle(IngestRequirementCommand command) {
        OriginalText originalText = new OriginalText(command.originalText());
        Provenance provenance = Provenance.create(
            ElementId.create(),
            originalText,
            SourceMetadata.apiRequest(),
            Instant.now()
        );
        RequirementIngestedEvent event = RequirementIngestedEvent.create(provenance);

        try {
            eventPublisher.publish(event);
        } catch (RuntimeException e) {
            outcomeStore.consume(event.correlationId());
            return IngestRequirementResult.internalError(
                event.correlationId(),
                "Requirement ingestion failed internally."
            );
        }

        return outcomeStore
            .consume(event.correlationId())
            .orElseGet(() -> IngestRequirementResult.internalError(
                event.correlationId(),
                "No final ingestion outcome was recorded."
            ));
    }

    @Override
    public Class<IngestRequirementCommand> commandType() {
        return IngestRequirementCommand.class;
    }
}
