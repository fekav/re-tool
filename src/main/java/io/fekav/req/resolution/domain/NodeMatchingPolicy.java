package io.fekav.req.resolution.domain;

import io.fekav.req.shared.model.CandidateNodeMatch;

public interface NodeMatchingPolicy {

    NodeMatchingResult decide(CandidateNodeMatch match);
}
