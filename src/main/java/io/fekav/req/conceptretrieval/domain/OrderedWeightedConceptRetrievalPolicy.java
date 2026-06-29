package io.fekav.req.conceptretrieval.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.fekav.req.conceptretrieval.application.CandidateLookupScope;
import io.fekav.req.conceptretrieval.application.ConceptCandidateLookup;

public class OrderedWeightedConceptRetrievalPolicy implements ConceptRetrievalPolicy {

    public static final String POLICY_NAME = "orderedWeighted";
    public static final String EXACT_LABEL_LOOKUP = "exactLabel";
    public static final String ALIAS_LOOKUP = "alias";
    public static final double EXACT_LABEL_WEIGHT = 1.0;
    public static final double ALIAS_WEIGHT = 0.7;

    private static final double FULL_SCORE_THRESHOLD = 1.0;
    private static final double WEIGHT_TOLERANCE = 0.000_000_001;

    private final List<ConceptCandidateLookup> lookups;

    public OrderedWeightedConceptRetrievalPolicy(Collection<ConceptCandidateLookup> lookups) {
        if (lookups == null || lookups.isEmpty()) {
            throw new IllegalArgumentException("ordered weighted retrieval requires lookup methods");
        }

        List<ConceptCandidateLookup> copiedLookups = List.copyOf(lookups);
        validateLookupConfiguration(copiedLookups);
        this.lookups = copiedLookups
            .stream()
            .sorted(Comparator.comparingDouble(ConceptCandidateLookup::lookupWeight).reversed())
            .toList();
    }

    @Override
    public CandidateConceptMatch retrieveCandidates(SelectedTerm selectedTerm) {
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");

        CandidateLookupScope scope = CandidateLookupScope.forSelectedTerm(selectedTerm);
        Map<String, CandidateAggregate> candidateAggregates = new LinkedHashMap<>();
        int order = 0;

        for (ConceptCandidateLookup lookup : lookups) {
            List<CandidateLookupHit> lookupHits = List.copyOf(
                lookup.findCandidates(selectedTerm, scope)
            );

            if (isExactLookup(lookup)) {
                if (!lookupHits.isEmpty()) {
                    return candidateMatch(
                        selectedTerm,
                        aggregateExactHits(lookupHits)
                    );
                }
                continue;
            }

            for (CandidateLookupHit lookupHit : lookupHits) {
                CandidateAggregate candidateAggregate = candidateAggregates.computeIfAbsent(
                    lookupHit.candidate().candidateKey(),
                    ignored -> new CandidateAggregate(lookupHit.candidate(), candidateAggregates.size())
                );
                candidateAggregate.add(lookup, lookupHit, order++);
            }
        }

        if (candidateAggregates.isEmpty()) {
            return noMatch(selectedTerm);
        }

        return candidateMatch(
            selectedTerm,
            candidateAggregates.values()
        );
    }

    private void validateLookupConfiguration(List<ConceptCandidateLookup> configuredLookups) {
        Set<String> lookupNames = new LinkedHashSet<>();
        boolean hasExactLookup = false;
        boolean hasAliasLookup = false;
        double nonExactWeightSum = 0.0;

        for (ConceptCandidateLookup lookup : configuredLookups) {
            Objects.requireNonNull(lookup, "lookup method must not be null");
            String lookupMethodName = lookup.lookupMethodName();
            if (lookupMethodName == null || lookupMethodName.isBlank()) {
                throw new IllegalArgumentException("lookup method name must not be blank");
            }
            if (!lookupNames.add(lookupMethodName.strip())) {
                throw new IllegalArgumentException("lookup method names must be unique");
            }

            double lookupWeight = lookup.lookupWeight();
            if (Double.isNaN(lookupWeight) || lookupWeight <= 0.0 || lookupWeight > 1.0) {
                throw new IllegalArgumentException(
                    "lookup method weight must be greater than 0.0 and at most 1.0"
                );
            }

            if (EXACT_LABEL_LOOKUP.equals(lookupMethodName)) {
                hasExactLookup = true;
                requireWeight(lookup, EXACT_LABEL_WEIGHT);
            } else {
                nonExactWeightSum += lookupWeight;
            }

            if (ALIAS_LOOKUP.equals(lookupMethodName)) {
                hasAliasLookup = true;
                requireWeight(lookup, ALIAS_WEIGHT);
            }
        }

        if (!hasExactLookup) {
            throw new IllegalArgumentException(
                "ordered weighted retrieval requires exactLabel lookup"
            );
        }
        if (!hasAliasLookup) {
            throw new IllegalArgumentException("ordered weighted retrieval requires alias lookup");
        }
        if (nonExactWeightSum >= FULL_SCORE_THRESHOLD - WEIGHT_TOLERANCE) {
            throw new IllegalArgumentException(
                "non-exact lookup weights must sum to less than 1.0"
            );
        }
    }

    private void requireWeight(ConceptCandidateLookup lookup, double expectedWeight) {
        if (Math.abs(lookup.lookupWeight() - expectedWeight) > WEIGHT_TOLERANCE) {
            throw new IllegalArgumentException(
                lookup.lookupMethodName() + " lookup weight must be " + expectedWeight
            );
        }
    }

    private Collection<CandidateAggregate> aggregateExactHits(List<CandidateLookupHit> lookupHits) {
        Map<String, CandidateAggregate> exactAggregates = new LinkedHashMap<>();
        for (CandidateLookupHit lookupHit : lookupHits) {
            CandidateAggregate candidateAggregate = exactAggregates.computeIfAbsent(
                lookupHit.candidate().candidateKey(),
                ignored -> new CandidateAggregate(lookupHit.candidate(), exactAggregates.size())
            );
            candidateAggregate.add(EXACT_LABEL_LOOKUP, EXACT_LABEL_WEIGHT, lookupHit);
        }
        return exactAggregates.values();
    }

    private CandidateConceptMatch candidateMatch(
        SelectedTerm selectedTerm,
        Collection<CandidateAggregate> candidateAggregates
    ) {
        List<RetrievedCandidateConcept> candidates = candidateAggregates
            .stream()
            .sorted()
            .map(CandidateAggregate::toRetrievedCandidate)
            .toList();

        return new CandidateConceptMatch(
            selectedTerm,
            candidates
        );
    }

    private CandidateConceptMatch noMatch(SelectedTerm selectedTerm) {
        return new CandidateConceptMatch(selectedTerm, List.of());
    }

    private boolean isExactLookup(ConceptCandidateLookup lookup) {
        return EXACT_LABEL_LOOKUP.equals(lookup.lookupMethodName());
    }

    private static double normalizeScore(double score) {
        return Math.round(score * 1_000_000_000.0) / 1_000_000_000.0;
    }

    private static final class CandidateAggregate implements Comparable<CandidateAggregate> {

        private final CandidateConcept candidate;
        private final int firstCandidateOrder;
        private final Set<String> countedLookupMethodNames = new LinkedHashSet<>();
        private final List<String> evidenceTexts = new ArrayList<>();
        private double score;
        private int firstLookupOrder = Integer.MAX_VALUE;

        private CandidateAggregate(CandidateConcept candidate, int firstCandidateOrder) {
            this.candidate = candidate;
            this.firstCandidateOrder = firstCandidateOrder;
        }

        private void add(
            ConceptCandidateLookup lookup,
            CandidateLookupHit lookupHit,
            int lookupOrder
        ) {
            add(lookup.lookupMethodName(), lookup.lookupWeight(), lookupHit);
            firstLookupOrder = Math.min(firstLookupOrder, lookupOrder);
        }

        private void add(
            String lookupMethodName,
            double lookupWeight,
            CandidateLookupHit lookupHit
        ) {
            if (countedLookupMethodNames.add(lookupMethodName)) {
                score += lookupWeight;
                evidenceTexts.add(lookupHit.evidenceText());
            }
        }

        private double score() {
            return normalizeScore(score);
        }

        private RetrievedCandidateConcept toRetrievedCandidate() {
            RetrievalEvidence evidence = new RetrievalEvidence(
                POLICY_NAME,
                String.join("; ", evidenceTexts),
                score()
            );
            return new RetrievedCandidateConcept(candidate, List.of(evidence));
        }

        @Override
        public int compareTo(CandidateAggregate other) {
            int lookupOrderComparison = Integer.compare(firstLookupOrder, other.firstLookupOrder);
            if (lookupOrderComparison != 0) {
                return lookupOrderComparison;
            }
            return Integer.compare(firstCandidateOrder, other.firstCandidateOrder);
        }
    }
}
