package io.fekav.req.conceptretrieval.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record CandidateConceptMatchSet(
    List<CandidateConceptMatch> matches
) {

    public CandidateConceptMatchSet {
        Objects.requireNonNull(matches, "candidate concept matches must not be null");

        Set<TermKey> selectedTerms = new HashSet<>();
        for (CandidateConceptMatch match : matches) {
            Objects.requireNonNull(match, "candidate concept match must not be null");
            if (!selectedTerms.add(new TermKey(match.syntaxRole(), match.text()))) {
                throw new IllegalArgumentException(
                    "candidate concept match set must contain one entry per selected term"
                );
            }
        }

        matches = List.copyOf(matches);
    }

    private record TermKey(String syntaxRole, String text) {
    }
}
