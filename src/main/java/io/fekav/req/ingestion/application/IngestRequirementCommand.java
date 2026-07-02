package io.fekav.req.ingestion.application;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.shared.event.RequirementIngestedEvent;

public record IngestRequirementCommand(
    String originalText
) implements Command<RequirementIngestedEvent> {

    public IngestRequirementCommand {
        if (originalText == null || originalText.isBlank()) {
            throw new IllegalArgumentException("Ingestion command has no original text");
        }
    }
}
