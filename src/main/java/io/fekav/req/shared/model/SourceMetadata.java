package io.fekav.req.shared.model;

import java.util.Objects;

public record SourceMetadata(SourceName sourceName) {

    public SourceMetadata {
        Objects.requireNonNull(sourceName, "sourceName must not be null");
    }

    public static SourceMetadata apiRequest() {
        return new SourceMetadata(SourceName.apiRequest());
    }
}
