package io.fekav.req.classification.application;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.shared.event.RequirementClassifiedEvent;

public record ClassifyRequirementCommand(
    String rawText
) implements Command<RequirementClassifiedEvent> {

    public ClassifyRequirementCommand {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Classification command has no raw text");
        }
    }
}
