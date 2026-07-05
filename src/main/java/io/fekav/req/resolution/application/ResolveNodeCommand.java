package io.fekav.req.resolution.application;

import java.util.Objects;

import io.fekav.platform.cqrs.Command;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.shared.event.NodeResolutionEvent;
import io.fekav.req.shared.model.RequirementElement;

public record ResolveNodeCommand(
    CorrelationId correlationId,
    RequirementElement requirementElement
) implements Command<NodeResolutionEvent> {

    public ResolveNodeCommand {
        correlationId = correlationId == null
            ? CorrelationId.create()
            : correlationId;
        Objects.requireNonNull(requirementElement, "requirementElement must not be null");
    }

    public ResolveNodeCommand(RequirementElement requirementElement) {
        this(CorrelationId.create(), requirementElement);
    }
}
