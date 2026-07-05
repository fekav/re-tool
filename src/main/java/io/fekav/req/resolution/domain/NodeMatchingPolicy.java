package io.fekav.req.resolution.domain;

import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.NodeMatchDecision;

public interface NodeMatchingPolicy {

    NodeMatchDecision decide(CandidateNodeMatch match);
}
