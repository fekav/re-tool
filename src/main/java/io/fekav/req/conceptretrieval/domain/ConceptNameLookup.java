package io.fekav.req.conceptretrieval.domain;

import java.util.List;

public interface ConceptNameLookup {

    List<CandidateConcept> findCandidates(SelectedTerm selectedTerm);
}
