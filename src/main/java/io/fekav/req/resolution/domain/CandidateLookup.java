package io.fekav.req.resolution.domain;

import java.util.List;

import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.RequirementElement;

public interface CandidateLookup {

    List<CandidateConcept> findCandidates(RequirementElement selectedTerm);
}
