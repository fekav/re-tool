package io.fekav.req.entityextraction.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.entityextraction.domain.RequirementSyntax;
import io.fekav.req.shared.model.Requirement;
import io.fekav.req.shared.model.RawRequirementText;
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

    private final EventPublisher eventPublisher;
    private final RequirementSyntaxExtraction requirementSyntaxExtraction;

    @Inject
    public ExtractEntitiesCommandHandler(
        EventPublisher eventPublisher,
        RequirementSyntaxExtraction requirementSyntaxExtraction
    ) {
        this.eventPublisher = eventPublisher;
        this.requirementSyntaxExtraction = requirementSyntaxExtraction;
    }

    @Override
    @Transactional
    public RequirementSyntax handle(ExtractEntitiesCommand command) {
        RawRequirementText rawRequirementText = new RawRequirementText(command.rawText());
        Requirement requirement = Requirement.create(rawRequirementText);

        RequirementSyntax requirementSyntax =
            requirementSyntaxExtraction.extractRequirementSyntax(rawRequirementText);
        requirement.applyExtraction(requirementSyntax);

        eventPublisher.publishAll(requirement.domainEvents());

        return requirementSyntax;
    }

    @Override
    public Class<ExtractEntitiesCommand> commandType() {
        return ExtractEntitiesCommand.class;
    }

}
