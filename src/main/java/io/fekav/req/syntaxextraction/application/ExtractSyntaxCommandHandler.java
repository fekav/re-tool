package io.fekav.req.syntaxextraction.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.shared.model.Requirement;
import io.fekav.req.syntaxextraction.domain.RequirementSyntax;
import io.fekav.req.shared.model.RawText;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Command Handler (Use Case implementation)
 * 
 */
@ApplicationScoped
public class ExtractSyntaxCommandHandler
        implements CommandHandler<RequirementSyntax, ExtractSyntaxCommand> {

    private final EventPublisher eventPublisher;
    private final RequirementSyntaxExtraction requirementSyntaxExtraction;

    @Inject
    public ExtractSyntaxCommandHandler(
        EventPublisher eventPublisher,
        RequirementSyntaxExtraction requirementSyntaxExtraction
    ) {
        this.eventPublisher = eventPublisher;
        this.requirementSyntaxExtraction = requirementSyntaxExtraction;
    }

    @Override
    @Transactional
    public RequirementSyntax handle(ExtractSyntaxCommand command) {
        RawText rawRequirementText = new RawText(command.rawText());
        Requirement requirement = Requirement.create(rawRequirementText);

        RequirementSyntax requirementSyntax =
            requirementSyntaxExtraction.extractRequirementSyntax(rawRequirementText);
        requirement.applyExtraction(requirementSyntax);

        eventPublisher.publishAll(requirement.domainEvents());

        return requirementSyntax;
    }

    @Override
    public Class<ExtractSyntaxCommand> commandType() {
        return ExtractSyntaxCommand.class;
    }

}
