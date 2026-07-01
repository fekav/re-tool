package io.fekav.req.syntaxextraction.application;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.shared.event.SyntaxExtractedEvent;

public record ExtractSyntaxCommand(
    String rawText
) implements Command<SyntaxExtractedEvent> {
    public ExtractSyntaxCommand {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Extraction command has no raw text");
        }
    }
}
