package io.fekav.req.entityextraction.application;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.entityextraction.domain.RequirementSyntax;

public record ExtractEntitiesCommand(
    String rawText
) implements Command<RequirementSyntax> {
    public ExtractEntitiesCommand {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Extraction command has no raw text");
        }
    }
}
