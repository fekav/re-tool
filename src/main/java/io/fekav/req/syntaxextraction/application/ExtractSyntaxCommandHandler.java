package io.fekav.req.syntaxextraction.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
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
        implements CommandHandler<RequirementElementsExtractedEvent, ExtractSyntaxCommand> {

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
    public RequirementElementsExtractedEvent handle(ExtractSyntaxCommand command) {
        RawText rawRequirementText = new RawText(command.rawText());
        Action action = syntaxExtraction.extractSyntax(rawRequirementText);
        RequirementElementsExtractedEvent event = RequirementElementsExtractedEvent.create(
            command.correlationId(),
            rawRequirementText,
            action
        );

        eventPublisher.publish(event);

        return event;
    }

    @Override
    public Class<ExtractSyntaxCommand> commandType() {
        return ExtractSyntaxCommand.class;
    }

}
