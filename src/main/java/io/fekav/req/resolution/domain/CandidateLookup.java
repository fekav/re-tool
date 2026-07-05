package io.fekav.req.resolution.domain;

import java.util.List;

import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.RequirementElement;

public interface CandidateLookup {

    List<CandidateNode> findCandidates(RequirementElement selectedTerm);
}
