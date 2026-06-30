package io.fekav.req.conceptmatching.domain;

import io.fekav.req.shared.model.CandidateConceptMatch;

public interface ConceptMatchingPolicy {

    ConceptMatchDecision decide(CandidateConceptMatch match);
}
