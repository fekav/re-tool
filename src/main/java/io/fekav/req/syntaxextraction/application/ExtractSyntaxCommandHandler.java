package io.fekav.req.syntaxextraction.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.shared.model.Requirement;
import io.fekav.req.syntaxextraction.domain.Action;
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
        implements CommandHandler<ExtractSyntaxResponse, ExtractSyntaxCommand> {

    private final EventPublisher eventPublisher;
    private final SyntaxExtraction syntaxExtraction;

    @Inject
    public ExtractSyntaxCommandHandler(
        EventPublisher eventPublisher,
        SyntaxExtraction syntaxExtraction
    ) {
        this.eventPublisher = eventPublisher;
        this.syntaxExtraction = syntaxExtraction;
    }

    @Override
    @Transactional
    public ExtractSyntaxResponse handle(ExtractSyntaxCommand command) {
        RawText rawRequirementText = new RawText(command.rawText());
        Requirement requirement = Requirement.create(rawRequirementText);

        Action action = syntaxExtraction.extractSyntax(rawRequirementText);
        requirement.applyExtraction(action);

        eventPublisher.publishAll(requirement.domainEvents());

        return ExtractSyntaxResponse.from(action);
    }

    @Override
    public Class<ExtractSyntaxCommand> commandType() {
        return ExtractSyntaxCommand.class;
    }

}
