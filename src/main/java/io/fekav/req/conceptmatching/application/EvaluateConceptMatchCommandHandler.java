package io.fekav.req.conceptmatching.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.conceptmatching.domain.ConceptMatchDecision;
import io.fekav.req.conceptmatching.domain.ConceptMatchingService;
import io.fekav.req.shared.event.ConceptMatchEvaluatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EvaluateConceptMatchCommandHandler
        implements CommandHandler<ConceptMatchEvaluatedEvent, EvaluateConceptMatchCommand> {

    private final EventPublisher eventPublisher;
    private final ConceptMatchingService conceptMatchingService;

    @Inject
    public EvaluateConceptMatchCommandHandler(
        EventPublisher eventPublisher,
        ConceptMatchingService conceptMatchingService
    ) {
        this.eventPublisher = eventPublisher;
        this.conceptMatchingService = conceptMatchingService;
    }

    @Override
    public ConceptMatchEvaluatedEvent handle(EvaluateConceptMatchCommand command) {
        ConceptMatchDecision decision = conceptMatchingService.evaluateMatch(command.match());
        ConceptMatchEvaluatedEvent event =
            ConceptMatchEvaluatedEvent.create(command.match(), decision);

        eventPublisher.publish(event);

        return event;
    }

    @Override
    public Class<EvaluateConceptMatchCommand> commandType() {
        return EvaluateConceptMatchCommand.class;
    }
}
