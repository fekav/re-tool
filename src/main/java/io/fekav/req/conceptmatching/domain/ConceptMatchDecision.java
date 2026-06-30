package io.fekav.req.conceptmatching.domain;

import java.util.List;
import java.util.Objects;

import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.SelectedTerm;

public record ConceptMatchDecision(
    SelectedTerm selectedTerm,
    ConceptMatchDecisionStatus status,
    List<RetrievedCandidateConcept> candidates,
    List<NewConceptProposal> newConcepts,
    String rationale
) {

    public ConceptMatchDecision {
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(candidates, "concept match decision candidates must not be null");
        Objects.requireNonNull(newConcepts, "concept match decision new concepts must not be null");
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("concept match decision rationale must not be blank");
        }

        if (candidates.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException(
                "concept match decision candidates must not contain null"
            );
        }
        candidates = List.copyOf(candidates);

        if (newConcepts.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException(
                "concept match decision new concepts must not contain null"
            );
        }
        newConcepts = List.copyOf(newConcepts);

        rationale = rationale.strip();
        validatePayload(status, candidates, newConcepts);
    }

    private static void validatePayload(
        ConceptMatchDecisionStatus status,
        List<RetrievedCandidateConcept> candidates,
        List<NewConceptProposal> newConcepts
    ) {
        switch (status) {
            case AUTO_MAP_EXISTING -> validateExistingCandidateDecision(
                candidates,
                newConcepts,
                "auto-map decisions"
            );
            case PROPOSE_EXISTING -> validateExistingCandidateDecision(
                candidates,
                newConcepts,
                "propose-existing decisions"
            );
            case REVIEW_REQUIRED -> validateReviewRequiredDecision(candidates, newConcepts);
            case AUTO_CREATE_NEW -> validateAutoCreateDecision(candidates, newConcepts);
        }
    }

    private static void validateExistingCandidateDecision(
        List<RetrievedCandidateConcept> candidates,
        List<NewConceptProposal> newConcepts,
        String decisionName
    ) {
        if (candidates.size() != 1) {
            throw new IllegalArgumentException(
                decisionName + " must contain exactly one candidate"
            );
        }
        if (!newConcepts.isEmpty()) {
            throw new IllegalArgumentException(
                decisionName + " must not contain new concepts"
            );
        }
    }

    private static void validateReviewRequiredDecision(
        List<RetrievedCandidateConcept> candidates,
        List<NewConceptProposal> newConcepts
    ) {
        if (candidates.size() < 2) {
            throw new IllegalArgumentException(
                "review-required decisions must contain at least two candidates"
            );
        }
        if (!newConcepts.isEmpty()) {
            throw new IllegalArgumentException(
                "review-required decisions must not contain new concepts"
            );
        }
    }

    private static void validateAutoCreateDecision(
        List<RetrievedCandidateConcept> candidates,
        List<NewConceptProposal> newConcepts
    ) {
        if (!candidates.isEmpty()) {
            throw new IllegalArgumentException(
                "auto-create-new decisions must not contain candidates"
            );
        }
        if (newConcepts.size() != 1) {
            throw new IllegalArgumentException(
                "auto-create-new decisions must contain exactly one new concept"
            );
        }
    }
}
