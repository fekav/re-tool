package io.fekav.req.conceptretrieval.application;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import io.fekav.req.conceptretrieval.domain.SelectedTerm;

public record CandidateLookupScope(
    String syntaxRole,
    Set<String> allowedConceptTypes
) {

    public CandidateLookupScope {
        if (syntaxRole == null || syntaxRole.isBlank()) {
            throw new IllegalArgumentException(
                "candidate lookup scope syntax role must not be blank"
            );
        }

        syntaxRole = syntaxRole.strip();
        allowedConceptTypes = copyAllowedConceptTypes(allowedConceptTypes);
    }

    public static CandidateLookupScope forSelectedTerm(SelectedTerm selectedTerm) {
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");
        return new CandidateLookupScope(selectedTerm.syntaxRole(), Set.of());
    }

    private static Set<String> copyAllowedConceptTypes(Set<String> allowedConceptTypes) {
        if (allowedConceptTypes == null || allowedConceptTypes.isEmpty()) {
            return Set.of();
        }

        Set<String> copiedAllowedConceptTypes = new LinkedHashSet<>();
        for (String allowedConceptType : allowedConceptTypes) {
            if (allowedConceptType == null || allowedConceptType.isBlank()) {
                throw new IllegalArgumentException(
                    "candidate lookup scope allowed concept type must not be blank"
                );
            }
            copiedAllowedConceptTypes.add(allowedConceptType.strip());
        }
        return Collections.unmodifiableSet(copiedAllowedConceptTypes);
    }
}
