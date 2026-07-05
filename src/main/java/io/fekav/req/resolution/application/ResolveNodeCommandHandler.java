package io.fekav.req.resolution.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.resolution.domain.NodeMatchingService;
import io.fekav.req.resolution.domain.NodeRetrievalService;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.NodeMatchDecision;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ResolveNodeCommandHandler
        implements CommandHandler<NodeResolutionDecidedEvent, ResolveNodeCommand> {

    private final EventPublisher eventPublisher;
    private final NodeRetrievalService nodeRetrievalService;
    private final NodeMatchingService nodeMatchingService;

    @Inject
    public ResolveNodeCommandHandler(
        EventPublisher eventPublisher,
        NodeRetrievalService nodeRetrievalService,
        NodeMatchingService nodeMatchingService
    ) {
        this.eventPublisher = eventPublisher;
        this.nodeRetrievalService = nodeRetrievalService;
        this.nodeMatchingService = nodeMatchingService;
    }

    @Override
    public NodeResolutionDecidedEvent handle(ResolveNodeCommand command) {
        CandidateNodeMatch match =
            nodeRetrievalService.retrieveCandidates(command.requirementElement());
        NodeMatchDecision decision = nodeMatchingService.evaluateMatch(match);
        NodeResolutionDecidedEvent event =
            NodeResolutionDecidedEvent.create(command.correlationId(), decision);

        eventPublisher.publish(event);

        return event;
    }

    @Override
    public Class<ResolveNodeCommand> commandType() {
        return ResolveNodeCommand.class;
    }
}
