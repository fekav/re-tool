package io.fekav.req.resolution.domain;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.ConceptMatchDecision;

public interface ConceptMatchingPolicy {

    ConceptMatchDecision decide(CandidateConceptMatch match);
}
