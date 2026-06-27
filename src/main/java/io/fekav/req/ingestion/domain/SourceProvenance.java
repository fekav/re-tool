package io.fekav.req.ingestion.domain;

public record SourceProvenance(
    String sourceType,
    String sourceId
) {

    public SourceProvenance {
        if (sourceType == null || sourceType.isBlank()) {
            throw new InvalidRequirementIngestionException("source type must not be blank");
        }
        if (sourceId == null || sourceId.isBlank()) {
            throw new InvalidRequirementIngestionException("source id must not be blank");
        }

        sourceType = sourceType.strip();
        sourceId = sourceId.strip();
    }
}
