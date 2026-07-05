package io.fekav.req.resolution.domain;

import java.util.Objects;

import io.fekav.req.shared.model.NodeMatchDecision;
import io.fekav.req.shared.model.NodeMatchReviewRequest;
import io.fekav.req.shared.model.RequirementElement;

public sealed interface NodeMatchingResult
        permits NodeMatchingResult.Decided, NodeMatchingResult.ReviewRequired {

    RequirementElement requirementElement();

    static NodeMatchingResult decided(NodeMatchDecision decision) {
        return new Decided(decision);
    }

    static NodeMatchingResult reviewRequired(NodeMatchReviewRequest reviewRequest) {
        return new ReviewRequired(reviewRequest);
    }

    record Decided(NodeMatchDecision decision) implements NodeMatchingResult {

        public Decided {
            Objects.requireNonNull(decision, "decision must not be null");
        }

        @Override
        public RequirementElement requirementElement() {
            return decision.requirementElement();
        }
    }

    record ReviewRequired(
        NodeMatchReviewRequest reviewRequest
    ) implements NodeMatchingResult {

        public ReviewRequired {
            Objects.requireNonNull(reviewRequest, "reviewRequest must not be null");
        }

        @Override
        public RequirementElement requirementElement() {
            return reviewRequest.requirementElement();
        }
    }
}
