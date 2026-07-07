package io.fekav.req.review.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import io.fekav.platform.messaging.CorrelationId;
import io.fekav.req.shared.model.RequirementElement;

public record NodeMatchReviewId(String value) {

    private static final int TEXT_HASH_LENGTH = 16;

    public NodeMatchReviewId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("node match review id must not be blank");
        }
        value = value.strip();
    }

    public static NodeMatchReviewId from(
        CorrelationId correlationId,
        RequirementElement requirementElement
    ) {
        return new NodeMatchReviewId(
            correlationId.value() +
                "::" +
                requirementElement.type().name() +
                "::" +
                textHash(requirementElement.text())
        );
    }

    private static String textHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat
                .of()
                .formatHex(digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .substring(0, TEXT_HASH_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
