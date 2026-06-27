package io.fekav.req.ingestion.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.ingestion.domain.RequirementIngestion;
import io.fekav.req.ingestion.domain.SourceProvenance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IngestRequirementCommandHandler
        implements CommandHandler<RequirementIngestion, IngestRequirementCommand> {

    private final EventPublisher eventPublisher;
    private final RequirementTextNormalizationPolicy normalizationPolicy;

    @Inject
    public IngestRequirementCommandHandler(
        EventPublisher eventPublisher,
        RequirementTextNormalizationPolicy normalizationPolicy
    ) {
        this.eventPublisher = eventPublisher;
        this.normalizationPolicy = normalizationPolicy;
    }

    @Override
    @Transactional
    public RequirementIngestion handle(IngestRequirementCommand command) {
        String normalizedText = normalizationPolicy.normalize(command.rawText());
        RequirementIngestion ingestion = RequirementIngestion.create(
            new SourceProvenance(command.sourceType(), command.sourceId()),
            command.rawText(),
            normalizedText
        );

        eventPublisher.publishAll(ingestion.domainEvents());

        return ingestion;
    }

    @Override
    public Class<IngestRequirementCommand> commandType() {
        return IngestRequirementCommand.class;
    }
}
