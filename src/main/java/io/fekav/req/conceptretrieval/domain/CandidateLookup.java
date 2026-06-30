package io.fekav.req.conceptretrieval.domain;

import java.util.List;

import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.SelectedTerm;

public interface CandidateLookup {

    List<CandidateConcept> findCandidates(SelectedTerm selectedTerm);
}
