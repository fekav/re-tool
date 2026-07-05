package io.fekav.req.resolution.application;

import java.util.Objects;

import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.resolution.domain.NodeMatchingResult;
import io.fekav.req.shared.event.NodeResolutionDecidedEvent;
import io.fekav.req.shared.event.NodeResolutionEvent;
import io.fekav.req.shared.event.NodeResolutionReviewRequiredEvent;

final class NodeResolutionEventFactory {

    private NodeResolutionEventFactory() {
    }

    static NodeResolutionEvent from(
        CorrelationId correlationId,
        NodeMatchingResult result
    ) {
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(result, "node matching result must not be null");

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
