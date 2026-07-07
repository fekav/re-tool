package io.fekav.req.resolution.domain;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.fekav.req.shared.model.CandidateNode;
import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

public class TokenOverlapNodeRetrievalPolicy implements NodeRetrievalPolicy {

    public static final String POLICY_NAME = "tokenOverlap";
    public static final double MINIMUM_SCORE = 0.5;
    public static final double MAXIMUM_SCORE = 0.99;

    private static final Pattern TOKEN_SEPARATOR =
        Pattern.compile("[^\\p{L}\\p{N}]+");

    private final CompatibleCandidateLookup compatibleCandidateLookup;

    public TokenOverlapNodeRetrievalPolicy(
        CompatibleCandidateLookup compatibleCandidateLookup
    ) {
        this.compatibleCandidateLookup = Objects.requireNonNull(
            compatibleCandidateLookup,
            "compatibleCandidateLookup must not be null"
        );
    }

    @Override
    public CandidateNodeMatch retrieveCandidates(RequirementElement selectedTerm) {
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");

        if (selectedTerm.type() == RequirementElementType.ACTION) {
            return new CandidateNodeMatch(selectedTerm, List.of());
        }

        Set<String> queryTokens = uniqueTokens(selectedTerm.text());
        List<RetrievedCandidateNode> retrievedCandidates = compatibleCandidateLookup
            .findCompatibleCandidates(selectedTerm)
            .stream()
            .map(candidate -> scoredCandidate(selectedTerm, queryTokens, candidate))
            .filter(ScoredCandidate::reachesMinimumScore)
            .sorted(candidateOrder())
            .map(ScoredCandidate::retrievedCandidate)
            .toList();

        return new CandidateNodeMatch(selectedTerm, retrievedCandidates);
    }

    private ScoredCandidate scoredCandidate(
        RequirementElement selectedTerm,
        Set<String> queryTokens,
        CandidateNode candidate
    ) {
        double score = score(queryTokens, uniqueTokens(candidate.label()));
        RetrievalEvidence evidence = new RetrievalEvidence(
            POLICY_NAME,
            evidenceText(selectedTerm, candidate),
            score
        );
        return new ScoredCandidate(
            new RetrievedCandidateNode(candidate, List.of(evidence)),
            score
        );
    }

    private double score(Set<String> queryTokens, Set<String> candidateTokens) {
        return Optional
            .of(queryTokens)
            .filter(tokens -> !tokens.isEmpty())
            .map(tokens -> cappedScore(tokens, candidateTokens))
            .orElse(0.0);
    }

    private double cappedScore(Set<String> queryTokens, Set<String> candidateTokens) {
        long sharedTokens = queryTokens
            .stream()
            .filter(candidateTokens::contains)
            .count();
        return Math.min(
            MAXIMUM_SCORE,
            sharedTokens / (double) queryTokens.size()
        );
    }

    private Set<String> uniqueTokens(String text) {
        return TOKEN_SEPARATOR
            .splitAsStream(text.toLowerCase(Locale.ROOT))
            .filter(token -> !token.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Comparator<ScoredCandidate> candidateOrder() {
        return Comparator
            .comparingDouble(ScoredCandidate::score)
            .reversed()
            .thenComparing(candidate -> candidate.candidate().label())
            .thenComparing(candidate -> candidate.candidate().candidateKey());
    }

    private String evidenceText(
        RequirementElement selectedTerm,
        CandidateNode candidate
    ) {
        return "Matched selected term '" + selectedTerm.text() +
            "' to graph candidate '" + candidate.label() + "' by token overlap";
    }

    private record ScoredCandidate(
        RetrievedCandidateNode retrievedCandidate,
        double score
    ) {

        boolean reachesMinimumScore() {
            return score >= MINIMUM_SCORE;
        }

        CandidateNode candidate() {
            return retrievedCandidate.candidate();
        }
    }
}
