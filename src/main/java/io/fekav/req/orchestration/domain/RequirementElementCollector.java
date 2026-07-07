package io.fekav.req.orchestration.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import io.fekav.req.extraction.domain.Action;
import io.fekav.req.extraction.domain.Condition;
import io.fekav.req.extraction.domain.Constraint;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

public final class RequirementElementCollector {

    public List<RequirementElement> collectFrom(Action action) {
        Objects.requireNonNull(action, "action must not be null");

        Stream<RequirementElement> requiredElements = Stream.of(
            new RequirementElement(RequirementElementType.SUBJECT, action.subject().text()),
            new RequirementElement(RequirementElementType.ACTION, action.actionText()),
            new RequirementElement(RequirementElementType.OBJECT, action.targetObject().text())
        );
        Stream<RequirementElement> conditionElements = action.conditions()
            .stream()
            .sorted(Comparator.comparing(Condition::text))
            .map(condition -> new RequirementElement(
                RequirementElementType.CONDITION,
                condition.text()
            ));
        Stream<RequirementElement> constraintElements = action.constraints()
            .stream()
            .sorted(Comparator.comparing(Constraint::text))
            .map(constraint -> new RequirementElement(
                RequirementElementType.CONSTRAINT,
                constraint.text()
            ));

        return Stream.of(requiredElements, conditionElements, constraintElements)
            .flatMap(elements -> elements)
            .toList();
    }
}
