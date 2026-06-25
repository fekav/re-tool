package io.fekav.req.classification.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.classification.domain.RequirementClassification;
import io.fekav.req.shared.model.RawRequirementText;
import io.fekav.req.shared.model.Requirement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ClassifyRequirementCommandHandler
        implements CommandHandler<RequirementClassification, ClassifyRequirementCommand> {

    private final EventPublisher eventPublisher;
    private final RequirementClassificationService requirementClassificationService;

    @Inject
    public ClassifyRequirementCommandHandler(
        EventPublisher eventPublisher,
        RequirementClassificationService requirementClassificationService
    ) {
        this.eventPublisher = eventPublisher;
        this.requirementClassificationService = requirementClassificationService;
    }

    @Override
    @Transactional
    public RequirementClassification handle(ClassifyRequirementCommand command) {
        RawRequirementText rawRequirementText = new RawRequirementText(command.rawText());
        Requirement requirement = Requirement.create(rawRequirementText);

        RequirementClassification requirementClassification =
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
