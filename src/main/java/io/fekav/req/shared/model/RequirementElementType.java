package io.fekav.req.shared.model;

public enum RequirementElementType {
    SUBJECT("Subject"),
    ACTION("Action"),
    OBJECT("Object"),
    CONDITION("Condition"),
    CONSTRAINT("Constraint");

    private final String label;

    RequirementElementType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
