package io.fekav.req.conceptretrieval.domain;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.SelectedTerm;

public interface ConceptRetrievalPolicy {

    CandidateConceptMatch retrieveCandidates(SelectedTerm selectedTerm);
}
