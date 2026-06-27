package io.fekav.req.ingestion.application;

public interface RequirementTextNormalizationPolicy {
    String normalize(String rawText);
}
