package io.fekav.req.conceptretrieval.domain;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.RequirementElement;

public interface ConceptRetrievalPolicy {

    CandidateConceptMatch retrieveCandidates(RequirementElement selectedTerm);
}
