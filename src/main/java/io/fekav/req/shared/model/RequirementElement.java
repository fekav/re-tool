package io.fekav.req.shared.model;

public enum RequirementElement {
    SUBJECT("Subject"),
    ACTION("Action"),
    OBJECT("Object"),
    CONDITION("Condition"),
    CONSTRAINT("Constraint");

    private final String label;

    RequirementElement(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
