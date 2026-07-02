package io.fekav.req.classification.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.model.RawText;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ClassifyRequirementCommandHandler
        implements CommandHandler<RequirementClassifiedEvent, ClassifyRequirementCommand> {

    private final EventPublisher eventPublisher;
    private final ClassificationService requirementClassificationService;

    @Inject
    public ClassifyRequirementCommandHandler(
        EventPublisher eventPublisher,
        ClassificationService requirementClassificationService
    ) {
        this.eventPublisher = eventPublisher;
        this.requirementClassificationService = requirementClassificationService;
    }

    @Override
    @Transactional
    public RequirementClassifiedEvent handle(ClassifyRequirementCommand command) {
        RawText rawRequirementText = new RawText(command.rawText());
        Classification requirementClassification =
            requirementClassificationService.classifyRequirement(rawRequirementText);
        RequirementClassifiedEvent event =
            RequirementClassifiedEvent.create(
                command.correlationId(),
                rawRequirementText,
                requirementClassification
            );

        eventPublisher.publish(event);

        return event;
    }

    @Override
    public Class<ClassifyRequirementCommand> commandType() {
        return ClassifyRequirementCommand.class;
    }
}
