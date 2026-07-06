package io.fekav.req.resolution.application;

import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.platform.messaging.EventPublisher;
import io.fekav.req.resolution.domain.NodeMatchingResult;
import io.fekav.req.resolution.domain.NodeMatchingService;
import io.fekav.req.resolution.domain.NodeRetrievalService;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.event.NodeResolutionEvent;
import io.fekav.req.shared.event.NodeResolutionReviewRequiredEvent;
import io.fekav.req.shared.model.CandidateNodeMatch;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ResolveNodeCommandHandler
        implements CommandHandler<NodeResolutionEvent, ResolveNodeCommand> {

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
    public NodeResolutionEvent handle(ResolveNodeCommand command) {
        CandidateNodeMatch match =
            nodeRetrievalService.retrieveCandidates(command.requirementElement());
        NodeMatchingResult result = nodeMatchingService.evaluateMatch(match);
        NodeResolutionEvent event = eventFrom(
            command.correlationId(),
            result
        );

        eventPublisher.publish(event);

        return event;
    }

    @Override
    public Class<ResolveNodeCommand> commandType() {
        return ResolveNodeCommand.class;
    }

    private static NodeResolutionEvent eventFrom(
        CorrelationId correlationId,
        NodeMatchingResult result
    ) {
        return switch (result) {
            case NodeMatchingResult.Decided decided ->
                NodeResolutionDecidedEvent.create(correlationId, decided.decision());
            case NodeMatchingResult.ReviewRequired reviewRequired ->
                NodeResolutionReviewRequiredEvent.create(
                    correlationId,
                    reviewRequired.reviewRequest()
                );
        };
    }
}
