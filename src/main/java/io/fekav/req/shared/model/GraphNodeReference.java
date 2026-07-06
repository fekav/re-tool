package io.fekav.req.shared.model;

import java.util.Objects;

public record GraphNodeReference(
    NodeType nodeType,
    String key,
    String label
) {

    public GraphNodeReference {
        Objects.requireNonNull(nodeType, "graph node type must not be null");
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("graph node key must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("graph node label must not be blank");
        }

        key = key.strip();
        label = label.strip();
    }
}
