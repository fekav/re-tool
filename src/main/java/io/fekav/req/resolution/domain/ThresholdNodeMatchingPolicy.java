package io.fekav.req.resolution.domain;

import java.util.List;
import java.util.Objects;

import io.fekav.req.shared.model.CandidateNodeMatch;
import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchDecisionStatus;
import io.fekav.req.shared.model.NodeMatchReviewRequest;
import io.fekav.req.shared.model.RetrievalEvidence;
import io.fekav.req.shared.model.RetrievedCandidateNode;
import io.fekav.req.shared.model.RequirementElement;

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
    public NodeMatchingResult decide(CandidateNodeMatch match) {
        Objects.requireNonNull(match, "match must not be null");

        if (match.candidates().isEmpty()) {
            return autoCreateResult(match.requirementElement());
        }

        List<CandidateScore> candidateScores = scoreCandidates(match.candidates());
        double topScore = topScore(candidateScores);
        List<RetrievedCandidateNode> topCandidates = topCandidates(
            candidateScores,
            topScore
        );

        if (topScore >= autoMapThreshold) {
            if (topCandidates.size() == 1) {
                return autoMapExistingResult(
                    match.requirementElement(),
                    topCandidates,
                    topScore
                );
            }
            return reviewRequiredResult(
                match.requirementElement(),
                topCandidates,
                "Multiple top candidates share score " + topScore +
                    " at auto-map threshold " + autoMapThreshold +
                    "; human review is required."
            );
        }

        return reviewRequiredResult(
            match.requirementElement(),
            topCandidates,
            "Top candidate score " + topScore + " is below auto-map threshold " +
                autoMapThreshold + "; human review is required before mapping."
        );
    }

    private List<CandidateScore> scoreCandidates(
        List<RetrievedCandidateNode> candidates
    ) {
        return candidates
            .stream()
            .map(candidate -> new CandidateScore(candidate, bestScore(candidate)))
            .toList();
    }

    private double topScore(List<CandidateScore> candidateScores) {
        return candidateScores
            .stream()
            .mapToDouble(CandidateScore::score)
            .max()
            .orElseThrow();
    }

    private List<RetrievedCandidateNode> topCandidates(
        List<CandidateScore> candidateScores,
        double topScore
    ) {
        return candidateScores
            .stream()
            .filter(candidateScore ->
                Double.compare(candidateScore.score(), topScore) == 0
            )
            .map(CandidateScore::candidate)
            .toList();
    }

    private NodeMatchingResult autoMapExistingResult(
        RequirementElement requirementElement,
        List<RetrievedCandidateNode> candidates,
        double topScore
    ) {
        return NodeMatchingResult.decided(new NodeMatchDecision(
            requirementElement,
            NodeMatchDecisionStatus.AUTO_MAP_EXISTING,
            candidates,
            "Unique top candidate reached auto-map threshold " +
                autoMapThreshold + " with score " + topScore + "."
        ));
    }

    private NodeMatchingResult reviewRequiredResult(
        RequirementElement requirementElement,
        List<RetrievedCandidateNode> candidates,
        String rationale
    ) {
        return NodeMatchingResult.reviewRequired(new NodeMatchReviewRequest(
            requirementElement,
            candidates,
            rationale
        ));
    }

    private NodeMatchingResult autoCreateResult(RequirementElement requirementElement) {
        return NodeMatchingResult.decided(new NodeMatchDecision(
            requirementElement,
            NodeMatchDecisionStatus.AUTO_CREATE_NEW,
            List.of(),
            "No existing candidates found; auto-creating node from selected term."
        ));
    }

    private double bestScore(RetrievedCandidateNode candidate) {
        return candidate
            .evidence()
            .stream()
            .mapToDouble(RetrievalEvidence::score)
            .max()
            .orElseThrow();
    }

    private record CandidateScore(RetrievedCandidateNode candidate, double score) {
    }
}
