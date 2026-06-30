package io.fekav.req.conceptretrieval.domain;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.CandidateConceptMatchSet;
import io.fekav.req.shared.model.SelectedTerm;

public class ConceptRetrievalService {

    private final ConceptRetrievalPolicy retrievalPolicy;

    public ConceptRetrievalService(ConceptRetrievalPolicy retrievalPolicy) {
        this.retrievalPolicy = Objects.requireNonNull(
            retrievalPolicy,
            "retrievalPolicy must not be null"
        );
    }

    public CandidateConceptMatchSet retrieveCandidates(Collection<SelectedTerm> selectedTerms) {
        if (selectedTerms == null || selectedTerms.isEmpty()) {
            throw new IllegalArgumentException("selected terms must not be empty");
        }

        List<SelectedTerm> copiedSelectedTerms = List.copyOf(selectedTerms);
        if (copiedSelectedTerms.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("selected terms must not contain null");
        }

        List<CandidateConceptMatch> matches = copiedSelectedTerms
            .stream()
            .map(this::retrieveCandidateMatch)
            .toList();

        return new CandidateConceptMatchSet(matches);
    }

    private CandidateConceptMatch retrieveCandidateMatch(SelectedTerm selectedTerm) {
        CandidateConceptMatch match = retrievalPolicy.retrieveCandidates(selectedTerm);
        if (!selectedTerm.equals(match.selectedTerm())) {
            throw new IllegalStateException(
                "retrieval policy returned a match for a different selected term"
            );
        }
        return match;
    }
}
