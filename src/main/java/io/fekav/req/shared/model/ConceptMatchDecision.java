package io.fekav.req.shared.model;

import java.util.List;
import java.util.Objects;

public record ConceptMatchDecision(
    RequirementElement requirementElement,
    ConceptMatchDecisionStatus status,
    List<RetrievedCandidateConcept> candidates,
    String rationale
) {

    public ConceptMatchDecision {
        Objects.requireNonNull(requirementElement, "requirementElement must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(candidates, "concept match decision candidates must not be null");
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("concept match decision rationale must not be blank");
        }

        if (candidates.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException(
                "concept match decision candidates must not contain null"
            );
        }
        candidates = List.copyOf(candidates);

        rationale = rationale.strip();
        validatePayload(status, candidates);
    }

    private static void validatePayload(
        ConceptMatchDecisionStatus status,
        List<RetrievedCandidateConcept> candidates
    ) {
        switch (status) {
            case AUTO_MAP_EXISTING -> validateExistingCandidateDecision(
                candidates,
                "auto-map decisions"
            );
            case PROPOSE_EXISTING -> validateExistingCandidateDecision(
                candidates,
                "propose-existing decisions"
            );
            case REVIEW_REQUIRED -> validateReviewRequiredDecision(candidates);
            case AUTO_CREATE_NEW -> validateAutoCreateDecision(candidates);
        }
    }

    private static void validateExistingCandidateDecision(
        List<RetrievedCandidateConcept> candidates,
        String decisionName
    ) {
        if (candidates.size() != 1) {
            throw new IllegalArgumentException(
                decisionName + " must contain exactly one candidate"
            );
        }
    }

    private static void validateReviewRequiredDecision(
        List<RetrievedCandidateConcept> candidates
    ) {
        if (candidates.size() < 2) {
            throw new IllegalArgumentException(
                "review-required decisions must contain at least two candidates"
            );
        }
    }

    private static void validateAutoCreateDecision(
        List<RetrievedCandidateConcept> candidates
    ) {
        if (!candidates.isEmpty()) {
            throw new IllegalArgumentException(
                "auto-create-new decisions must not contain candidates"
            );
        }
    }
}
