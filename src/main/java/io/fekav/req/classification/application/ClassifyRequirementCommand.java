package io.fekav.req.classification.application;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.shared.event.RequirementClassifiedEvent;
import io.fekav.req.shared.model.CorrelationId;

public record ClassifyRequirementCommand(
    CorrelationId correlationId,
    String rawText
) implements Command<RequirementClassifiedEvent> {

    public ClassifyRequirementCommand {
        correlationId = correlationId == null
            ? CorrelationId.create()
            : correlationId;
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Classification command has no raw text");
        }
    }

    public ClassifyRequirementCommand(String rawText) {
        this(CorrelationId.create(), rawText);
    }
}
