package io.fekav.req.conceptretrieval.application;

import java.util.List;

import io.fekav.req.conceptretrieval.domain.CandidateLookupHit;
import io.fekav.req.conceptretrieval.domain.SelectedTerm;

public interface ConceptCandidateLookup {

    String lookupMethodName();

    double lookupWeight();

    List<CandidateLookupHit> findCandidates(
        SelectedTerm selectedTerm,
        CandidateLookupScope scope
    );
}
