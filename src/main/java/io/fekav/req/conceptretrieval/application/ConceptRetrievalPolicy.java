package io.fekav.req.conceptretrieval.application;

import java.util.Collection;

import io.fekav.req.conceptretrieval.domain.CandidateConceptMatchSet;

public interface ConceptRetrievalPolicy {

    CandidateConceptMatchSet retrieveCandidates(
        Collection<RetrieveCandidateConceptsCommand.SelectedTermInput> terms
    );
}
