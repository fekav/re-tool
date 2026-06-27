package io.fekav.req.ingestion.application;

import io.fekav.platform.cqrs.Command;
import io.fekav.req.ingestion.domain.RequirementIngestion;

public record IngestRequirementCommand(
    String sourceType,
    String sourceId,
    String rawText
) implements Command<RequirementIngestion> {

    public IngestRequirementCommand {
        if (sourceType == null || sourceType.isBlank()) {
            throw new IllegalArgumentException("Ingestion command has no source type");
        }
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("Ingestion command has no source id");
        }
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Ingestion command has no raw text");
        }
    }
}
