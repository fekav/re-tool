package io.fekav.req.conceptretrieval.domain;

public interface ConceptRetrievalPolicy {

    CandidateConceptMatch retrieveCandidates(SelectedTerm selectedTerm);
}
