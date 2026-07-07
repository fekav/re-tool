package io.fekav.req.resolution.domain;

import java.util.List;

import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.RequirementElement;

public interface CompatibleCandidateLookup {

    List<CandidateNode> findCompatibleCandidates(RequirementElement selectedTerm);
}
