package io.fekav.req.conceptretrieval.domain;

import java.util.Objects;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.SelectedTerm;

public class ConceptRetrievalService {

    private final ConceptRetrievalPolicy retrievalPolicy;

    public ConceptRetrievalService(ConceptRetrievalPolicy retrievalPolicy) {
        this.retrievalPolicy = Objects.requireNonNull(
            retrievalPolicy,
            "retrievalPolicy must not be null"
        );
    }

    public CandidateConceptMatch retrieveCandidates(SelectedTerm selectedTerm) {
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");
        CandidateConceptMatch match = retrievalPolicy.retrieveCandidates(selectedTerm);
        if (!selectedTerm.equals(match.selectedTerm())) {
            throw new IllegalStateException(
                "retrieval policy returned a match for a different selected term"
            );
        }
        return match;
    }
}
