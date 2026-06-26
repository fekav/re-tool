package io.fekav.req.classification.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.classification.domain.Classification;
import io.fekav.req.shared.model.RawText;
import io.fekav.req.shared.model.Requirement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ClassifyRequirementCommandHandler
        implements CommandHandler<Classification, ClassifyRequirementCommand> {

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
    public Classification handle(ClassifyRequirementCommand command) {
        RawText rawRequirementText = new RawText(command.rawText());
        Requirement requirement = Requirement.create(rawRequirementText);

        Classification requirementClassification =
            requirementClassificationService.classifyRequirement(rawRequirementText);
        requirement.applyClassification(requirementClassification);

        eventPublisher.publishAll(requirement.domainEvents());

        return requirementClassification;
    }

    @Override
    public Class<ClassifyRequirementCommand> commandType() {
        return ClassifyRequirementCommand.class;
    }
}
