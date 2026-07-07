package io.fekav.req.orchestration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.fekav.req.extraction.domain.Action;
import io.fekav.req.extraction.domain.Condition;
import io.fekav.req.extraction.domain.Constraint;
import io.fekav.req.extraction.domain.Subject;
import io.fekav.req.extraction.domain.TargetObject;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.shared.model.RequirementElement;
import io.fekav.req.shared.model.RequirementElementType;

class RequirementElementCollectorTest {

    private final RequirementElementCollector collector = new RequirementElementCollector();

    @Test
    void collectsSubjectActionObjectConditionAndConstraintElements_whenActionHasAllParts() {
        // Arrange
        Action action = new Action(
            ElementId.create(),
            "must validate",
            new Subject(ElementId.create(), "login form"),
            new TargetObject(ElementId.create(), "credentials"),
            Set.of(new Condition("before authentication")),
            Set.of(new Constraint("within 200 milliseconds"))
        );

        // Act
        List<RequirementElement> elements = collector.collectFrom(action);

        // Assert
        assertThat(elements).containsExactly(
            new RequirementElement(RequirementElementType.SUBJECT, "login form"),
            new RequirementElement(RequirementElementType.ACTION, "must validate"),
            new RequirementElement(RequirementElementType.OBJECT, "credentials"),
            new RequirementElement(RequirementElementType.CONDITION, "before authentication"),
            new RequirementElement(RequirementElementType.CONSTRAINT, "within 200 milliseconds")
        );
    }

    @Test
    void collectsOnlyRequiredElements_whenActionHasNoConditionsOrConstraints() {
        // Arrange
        Action action = new Action(
            ElementId.create(),
            "shall export",
            new Subject(ElementId.create(), "reporting dashboard"),
            new TargetObject(ElementId.create(), "monthly usage metrics"),
            Set.of(),
            Set.of()
        );

        // Act
        List<RequirementElement> elements = collector.collectFrom(action);

        // Assert
        assertThat(elements).containsExactly(
            new RequirementElement(RequirementElementType.SUBJECT, "reporting dashboard"),
            new RequirementElement(RequirementElementType.ACTION, "shall export"),
            new RequirementElement(RequirementElementType.OBJECT, "monthly usage metrics")
        );
    }
}
