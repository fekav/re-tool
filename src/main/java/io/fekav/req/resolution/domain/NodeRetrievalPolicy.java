package io.fekav.req.resolution.domain;

import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.RequirementElement;

public interface NodeRetrievalPolicy {

    CandidateNodeMatch retrieveCandidates(RequirementElement selectedTerm);
}
