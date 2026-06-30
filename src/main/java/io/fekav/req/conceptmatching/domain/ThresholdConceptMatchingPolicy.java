package io.fekav.req.conceptmatching.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.fekav.req.shared.model.CandidateConceptMatch;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateConcept;

public class ThresholdConceptMatchingPolicy implements ConceptMatchingPolicy {

    public static final double DEFAULT_AUTO_MAP_THRESHOLD = 1.0;

    private final double autoMapThreshold;

    public ThresholdConceptMatchingPolicy() {
        this(DEFAULT_AUTO_MAP_THRESHOLD);
    }

    public ThresholdConceptMatchingPolicy(double autoMapThreshold) {
        if (Double.isNaN(autoMapThreshold) || autoMapThreshold < 0.0 || autoMapThreshold > 1.0) {
            throw new IllegalArgumentException(
                "auto-map threshold must be between 0.0 and 1.0"
            );
        }
        this.autoMapThreshold = autoMapThreshold;
    }

    @Override
    public ConceptMatchDecision decide(CandidateConceptMatch match) {
        Objects.requireNonNull(match, "match must not be null");

        if (match.candidates().isEmpty()) {
            return autoCreateDecision(match);
        }

        double topScore = match
            .candidates()
            .stream()
            .mapToDouble(this::bestScore)
            .max()
            .orElseThrow();
        List<RetrievedCandidateConcept> topCandidates = match
            .candidates()
            .stream()
            .filter(candidate -> Double.compare(bestScore(candidate), topScore) == 0)
            .toList();

        if (topScore >= autoMapThreshold) {
            if (topCandidates.size() == 1) {
                return new ConceptMatchDecision(
                    match.selectedTerm(),
                    ConceptMatchDecisionStatus.AUTO_MAP_EXISTING,
                    topCandidates,
                    List.of(),
                    "Unique top candidate reached auto-map threshold " +
                        autoMapThreshold + " with score " + topScore + "."
                );
            }
            return new ConceptMatchDecision(
                match.selectedTerm(),
                ConceptMatchDecisionStatus.REVIEW_REQUIRED,
                topCandidates,
                List.of(),
                "Multiple top candidates share score " + topScore +
                    " at auto-map threshold " + autoMapThreshold +
                    "; human review is required."
            );
        }

        RetrievedCandidateConcept bestCandidate = match
            .candidates()
            .stream()
            .max(Comparator.comparingDouble(this::bestScore))
            .orElseThrow();
        return new ConceptMatchDecision(
            match.selectedTerm(),
            ConceptMatchDecisionStatus.PROPOSE_EXISTING,
            List.of(bestCandidate),
            List.of(),
            "Top candidate score " + topScore + " is below auto-map threshold " +
                autoMapThreshold + "; proposing best existing candidate."
        );
    }

    private ConceptMatchDecision autoCreateDecision(CandidateConceptMatch match) {
        return new ConceptMatchDecision(
            match.selectedTerm(),
            ConceptMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            List.of(new NewConceptProposal(
                match.selectedTerm().text(),
                match.selectedTerm().requirementElement()
            )),
            "No existing candidates found; auto-creating concept from selected term."
        );
    }

    private double bestScore(RetrievedCandidateConcept candidate) {
        return candidate
            .evidence()
            .stream()
            .mapToDouble(RetrievalEvidence::score)
            .max()
            .orElseThrow();
    }
}
