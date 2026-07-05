package io.fekav.req.resolution.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;

public class ThresholdNodeMatchingPolicy implements NodeMatchingPolicy {

    public static final double DEFAULT_AUTO_MAP_THRESHOLD = 1.0;

    private final double autoMapThreshold;

    public ThresholdNodeMatchingPolicy() {
        this(DEFAULT_AUTO_MAP_THRESHOLD);
    }

    public ThresholdNodeMatchingPolicy(double autoMapThreshold) {
        if (Double.isNaN(autoMapThreshold) || autoMapThreshold < 0.0 || autoMapThreshold > 1.0) {
            throw new IllegalArgumentException(
                "auto-map threshold must be between 0.0 and 1.0"
            );
        }
        this.autoMapThreshold = autoMapThreshold;
    }

    @Override
    public NodeMatchDecision decide(CandidateNodeMatch match) {
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
        List<RetrievedCandidateNode> topCandidates = match
            .candidates()
            .stream()
            .filter(candidate -> Double.compare(bestScore(candidate), topScore) == 0)
            .toList();

        if (topScore >= autoMapThreshold) {
            if (topCandidates.size() == 1) {
                return new NodeMatchDecision(
                    match.requirementElement(),
                    NodeMatchDecisionStatus.AUTO_MAP_EXISTING,
                    topCandidates,
                    "Unique top candidate reached auto-map threshold " +
                        autoMapThreshold + " with score " + topScore + "."
                );
            }
            return new NodeMatchDecision(
                match.requirementElement(),
                NodeMatchDecisionStatus.REVIEW_REQUIRED,
                topCandidates,
                "Multiple top candidates share score " + topScore +
                    " at auto-map threshold " + autoMapThreshold +
                    "; human review is required."
            );
        }

        RetrievedCandidateNode bestCandidate = match
            .candidates()
            .stream()
            .max(Comparator.comparingDouble(this::bestScore))
            .orElseThrow();
        return new NodeMatchDecision(
            match.requirementElement(),
            NodeMatchDecisionStatus.PROPOSE_EXISTING,
            List.of(bestCandidate),
            "Top candidate score " + topScore + " is below auto-map threshold " +
                autoMapThreshold + "; proposing best existing candidate."
        );
    }

    private NodeMatchDecision autoCreateDecision(CandidateNodeMatch match) {
        return new NodeMatchDecision(
            match.requirementElement(),
            NodeMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found; auto-creating node from selected term."
        );
    }

    private double bestScore(RetrievedCandidateNode candidate) {
        return candidate
            .evidence()
            .stream()
            .mapToDouble(RetrievalEvidence::score)
            .max()
            .orElseThrow();
    }
}
