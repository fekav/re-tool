package io.fekav.req.resolution.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.resolution.domain.ConceptMatchingService;
import io.fekav.req.resolution.domain.ConceptRetrievalService;
import io.fekav.req.shared.event.ConceptResolutionDecidedEvent;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.ConceptMatchDecision;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ResolveConceptCommandHandler
        implements CommandHandler<ConceptResolutionDecidedEvent, ResolveConceptCommand> {

    private final EventPublisher eventPublisher;
    private final ConceptRetrievalService conceptRetrievalService;
    private final ConceptMatchingService conceptMatchingService;

    @Inject
    public ResolveConceptCommandHandler(
        EventPublisher eventPublisher,
        ConceptRetrievalService conceptRetrievalService,
        ConceptMatchingService conceptMatchingService
    ) {
        this.eventPublisher = eventPublisher;
        this.conceptRetrievalService = conceptRetrievalService;
        this.conceptMatchingService = conceptMatchingService;
    }

    @Override
    public ConceptResolutionDecidedEvent handle(ResolveConceptCommand command) {
        CandidateConceptMatch match =
            conceptRetrievalService.retrieveCandidates(command.requirementElement());
        ConceptMatchDecision decision = conceptMatchingService.evaluateMatch(match);
        ConceptResolutionDecidedEvent event =
            ConceptResolutionDecidedEvent.create(command.correlationId(), decision);

        eventPublisher.publish(event);

        return event;
    }

    @Override
    public Class<ResolveConceptCommand> commandType() {
        return ResolveConceptCommand.class;
    }
}
