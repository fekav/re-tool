package io.fekav.req.shared.model;

import java.util.List;

public enum NodeMatchDecisionStatus {
    AUTO_MAP_EXISTING(
        true,
        "auto-map decisions must contain exactly one candidate"
    ),
    AUTO_CREATE_NEW(
        false,
        "auto-create-new decisions must not contain candidates"
    ),
    REVIEW_MAP_EXISTING(
        true,
        "review-map decisions must contain exactly one candidate"
    ),
    REVIEW_CREATE_NEW(
        false,
        "review-create-new decisions must not contain candidates"
    );

    private final boolean mapsExistingNode;
    private final String payloadValidationMessage;

    NodeMatchDecisionStatus(
        boolean mapsExistingNode,
        String payloadValidationMessage
    ) {
        this.mapsExistingNode = mapsExistingNode;
        this.payloadValidationMessage = payloadValidationMessage;
    }

    public boolean mapsExistingNode() {
        return mapsExistingNode;
    }

    void validatePayload(List<RetrievedCandidateNode> candidates) {
        if (mapsExistingNode && candidates.size() != 1) {
            throw new IllegalArgumentException(payloadValidationMessage);
        }

        if (!mapsExistingNode && !candidates.isEmpty()) {
            throw new IllegalArgumentException(payloadValidationMessage);
        }
    }
}
