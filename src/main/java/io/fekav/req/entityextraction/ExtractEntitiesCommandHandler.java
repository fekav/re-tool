package io.fekav.req.entityextraction;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.entityextraction.model.RequirementSyntax;
import io.fekav.req.shared.model.Requirement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Command Handler (Use Case implementation)
 * 
 */
@ApplicationScoped
public class ExtractEntitiesCommandHandler
        implements CommandHandler<RequirementSyntax, ExtractEntitiesCommand> {

    @Inject
    EventPublisher eventPublisher;

    @Inject
    EntityExtractionService extractionService;

    @Override
    @Transactional
    public RequirementSyntax handle(ExtractEntitiesCommand command) {
        // 1. create Aggregate
        Requirement requirement = Requirement.create(command.rawText());

        // 2. extract entities
        RequirementSyntax result = extractionService.extract(requirement);

        // 3. apply extraction to aggregate
        requirement.applyExtraction(result);

        // 4. publish Event
        eventPublisher.publishAll(requirement.domainEvents());

        return result;
    }

    @Override
    public Class<ExtractEntitiesCommand> commandType() {
        return ExtractEntitiesCommand.class;
    }

}
