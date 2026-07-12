package io.fekav.req.search.domain;

public record RequirementView(
    String rawText,
    String type,
    String property
) {

    public RequirementView {
        rawText = normalize(rawText, "rawText");
        type = normalize(type, "type");
        property = normalize(property, "property");
    }

    private static String normalize(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }
}
