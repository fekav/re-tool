package io.fekav.req.ingestion.application;

import io.fekav.platform.cqrs.Command;

public record IngestRequirementCommand(
    String originalText
) implements Command<IngestRequirementResult> {

    public IngestRequirementCommand {
        if (originalText == null || originalText.isBlank()) {
            throw new IllegalArgumentException("Ingestion command has no original text");
        }
    }
}
