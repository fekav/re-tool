package io.fekav.req.shared.model;

public record SourceName(String value) {

    private static final String API_REQUEST = "API-Request";

    public SourceName {
        if (value == null || value.isBlank()) {
            throw new InvalidSourceMetadataException("source name must not be blank");
        }
    }

    public static SourceName apiRequest() {
        return new SourceName(API_REQUEST);
    }
}
