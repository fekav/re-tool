package io.fekav.req.conceptretrieval.domain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.fekav.req.shared.model.CandidateConcept;
import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;
import io.fekav.req.shared.model.RequirementElement;

public class ConceptNameRetrievalPolicy implements ConceptRetrievalPolicy {

    public static final String POLICY_NAME = "conceptName";
    public static final double SCORE = 1.0;

    private final CandidateLookup candidateLookup;

    public ConceptNameRetrievalPolicy(CandidateLookup candidateLookup) {
        this.candidateLookup = Objects.requireNonNull(
            candidateLookup,
            "candidateLookup must not be null"
        );
    }

    @Override
    public CandidateConceptMatch retrieveCandidates(RequirementElement selectedTerm) {
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");

        List<CandidateConcept> candidates = deduplicatedCandidates(
            candidateLookup.findCandidates(selectedTerm)
        );
        List<RetrievedCandidateConcept> retrievedCandidates = candidates
            .stream()
            .map(candidate -> retrievedCandidate(selectedTerm, candidate))
            .toList();
        return new CandidateConceptMatch(selectedTerm, retrievedCandidates);
    }

    private RetrievedCandidateConcept retrievedCandidate(
        RequirementElement selectedTerm,
        CandidateConcept candidate
    ) {
        RetrievalEvidence evidence = new RetrievalEvidence(
            POLICY_NAME,
            evidenceText(selectedTerm, candidate),
            SCORE
        );
        return new RetrievedCandidateConcept(candidate, List.of(evidence));
    }

    private List<CandidateConcept> deduplicatedCandidates(Collection<CandidateConcept> candidates) {
        Map<String, CandidateConcept> candidatesByKey = new LinkedHashMap<>();
        for (CandidateConcept candidate : List.copyOf(candidates)) {
            candidatesByKey.putIfAbsent(candidate.candidateKey(), candidate);
        }
        return List.copyOf(candidatesByKey.values());
    }

    private String evidenceText(RequirementElement selectedTerm, CandidateConcept candidate) {
        return "Matched concept name '" + selectedTerm.text() + "' to graph candidate '" +
            candidate.label() + "'";
    }
}
