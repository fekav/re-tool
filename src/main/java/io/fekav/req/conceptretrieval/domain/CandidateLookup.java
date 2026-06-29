package io.fekav.req.conceptretrieval.domain;

import java.util.List;

public interface CandidateLookup {

    List<CandidateConcept> findCandidates(SelectedTerm selectedTerm);
}
