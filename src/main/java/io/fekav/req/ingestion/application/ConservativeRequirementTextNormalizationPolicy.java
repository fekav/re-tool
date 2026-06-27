package io.fekav.req.ingestion.application;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConservativeRequirementTextNormalizationPolicy
        implements RequirementTextNormalizationPolicy {

    @Override
    public String normalize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return rawText;
        }

        return rawText
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .strip();
    }
}
