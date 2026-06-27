package io.fekav.req.syntaxextraction.application;

import io.fekav.platform.cqrs.Command;

public record ExtractSyntaxCommand(
    String rawText
) implements Command<ExtractSyntaxResponse> {
    public ExtractSyntaxCommand {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Extraction command has no raw text");
        }
    }
}
