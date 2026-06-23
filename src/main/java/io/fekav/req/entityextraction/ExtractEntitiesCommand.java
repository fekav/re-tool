package io.fekav.req.entityextraction;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.entityextraction.model.RequirementSyntax;

public record ExtractEntitiesCommand(
    String rawText
) implements Command<RequirementSyntax> {
    public ExtractEntitiesCommand {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Extraction command has no raw text");
        }
    }
}
