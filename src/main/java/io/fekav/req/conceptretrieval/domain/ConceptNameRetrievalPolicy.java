package io.fekav.req.conceptretrieval.domain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public CandidateConceptMatch retrieveCandidates(SelectedTerm selectedTerm) {
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
        SelectedTerm selectedTerm,
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

    private String evidenceText(SelectedTerm selectedTerm, CandidateConcept candidate) {
        return "Matched concept name '" + selectedTerm.text() + "' to graph candidate '" +
            candidate.label() + "'";
    }
}
