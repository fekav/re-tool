package io.fekav.req.classification.application;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.classification.domain.RequirementClassification;

public record ClassifyRequirementCommand(
    String rawText
) implements Command<RequirementClassification> {

    public ClassifyRequirementCommand {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Classification command has no raw text");
        }
    }
}
