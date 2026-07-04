package io.fekav.req.resolution.application;

import java.util.Objects;

import io.fekav.platform.cqrs.Command;
import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.shared.event.ConceptResolutionDecidedEvent;
import io.fekav.req.shared.model.RequirementElement;

public record ResolveConceptCommand(
    CorrelationId correlationId,
    RequirementElement requirementElement
) implements Command<ConceptResolutionDecidedEvent> {

    public ResolveConceptCommand {
        correlationId = correlationId == null
            ? CorrelationId.create()
            : correlationId;
        Objects.requireNonNull(requirementElement, "requirementElement must not be null");
    }

    public ResolveConceptCommand(RequirementElement requirementElement) {
        this(CorrelationId.create(), requirementElement);
    }
}
