package io.fekav.req.extraction.application;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.shared.event.RequirementElementsExtractedEvent;
import io.fekav.platform.messaging.CorrelationId;

public record ExtractSyntaxCommand(
    CorrelationId correlationId,
    String rawText
) implements Command<RequirementElementsExtractedEvent> {
    public ExtractSyntaxCommand {
        correlationId = correlationId == null
            ? CorrelationId.create()
            : correlationId;
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Extraction command has no raw text");
        }
    }

    public ExtractSyntaxCommand(String rawText) {
        this(CorrelationId.create(), rawText);
    }
}
