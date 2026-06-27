package io.fekav.req.syntaxextraction.domain;

import java.util.Objects;
import java.util.Set;

import io.fekav.req.shared.model.ElementId;

public record Action(
    ElementId id,
    String actionText,
    Subject subject,
    TargetObject targetObject,
    Set<Condition> conditions,
    Set<Constraint> constraints
) {

    public Action {
        Objects.requireNonNull(id, "id must not be null");
        if (actionText == null || actionText.isBlank()) {
            throw new IllegalArgumentException("action text must not be blank");
        }
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(targetObject, "targetObject must not be null");
        Objects.requireNonNull(conditions, "conditions must not be null");
        Objects.requireNonNull(constraints, "constraints must not be null");

        actionText = actionText.strip();
        conditions = Set.copyOf(conditions);
        constraints = Set.copyOf(constraints);
    }
}
