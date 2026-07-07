package io.fekav.req.syntaxextraction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.fekav.req.extraction.domain.Action;
import io.fekav.req.extraction.domain.Condition;
import io.fekav.req.extraction.domain.Constraint;
import io.fekav.req.extraction.domain.Subject;
import io.fekav.req.extraction.domain.TargetObject;
import io.fekav.req.shared.model.ElementId;

class ActionTest {

    @Test
    void acceptsAction_whenRequiredElementsAreValid() {
        ElementId id = ElementId.create();
        Subject subject = subject();
        TargetObject targetObject = targetObject();

        Action action = new Action(
            id,
            " shall export ",
            subject,
            targetObject,
            Set.of(new Condition("on request")),
            Set.of(new Constraint("as CSV"))
        );

        assertThat(action.id()).isEqualTo(id);
        assertThat(action.actionText()).isEqualTo("shall export");
        assertThat(action.subject()).isEqualTo(subject);
        assertThat(action.targetObject()).isEqualTo(targetObject);
        assertThat(action.conditions()).containsExactly(new Condition("on request"));
        assertThat(action.constraints()).containsExactly(new Constraint("as CSV"));
    }

    @Test
    void copiesConditionAndConstraintSets() {
        Set<Condition> conditions = new HashSet<>();
        conditions.add(new Condition("on request"));
        Set<Constraint> constraints = new HashSet<>();
        constraints.add(new Constraint("as CSV"));

        Action action = new Action(
            ElementId.create(),
            "shall export",
            subject(),
            targetObject(),
            conditions,
            constraints
        );
        conditions.add(new Condition("after approval"));
        constraints.add(new Constraint("within 24 hours"));

        assertThat(action.conditions()).containsExactly(new Condition("on request"));
        assertThat(action.constraints()).containsExactly(new Constraint("as CSV"));
    }

    @Test
    void deduplicatesConditionAndConstraintValueObjects() {
        Set<Condition> conditions = new HashSet<>();
        conditions.add(new Condition("on request"));
        conditions.add(new Condition(" on request "));
        Set<Constraint> constraints = new HashSet<>();
        constraints.add(new Constraint("as CSV"));
        constraints.add(new Constraint(" as CSV "));

        Action action = new Action(
            ElementId.create(),
            "shall export",
            subject(),
            targetObject(),
            conditions,
            constraints
        );

        assertThat(action.conditions()).containsExactly(new Condition("on request"));
        assertThat(action.constraints()).containsExactly(new Constraint("as CSV"));
    }

    @Test
    void rejectsAction_whenIdIsNull() {
        assertThatThrownBy(() -> new Action(
            null,
            "shall export",
            subject(),
            targetObject(),
            Set.of(),
            Set.of()
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("id must not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void rejectsAction_whenActionTextIsBlank(String actionText) {
        assertThatThrownBy(() -> new Action(
            ElementId.create(),
            actionText,
            subject(),
            targetObject(),
            Set.of(),
            Set.of()
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("action text must not be blank");
    }

    @Test
    void rejectsAction_whenActionTextIsNull() {
        assertThatThrownBy(() -> new Action(
            ElementId.create(),
            null,
            subject(),
            targetObject(),
            Set.of(),
            Set.of()
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("action text must not be blank");
    }

    @Test
    void rejectsAction_whenSubjectIsNull() {
        assertThatThrownBy(() -> new Action(
            ElementId.create(),
            "shall export",
            null,
            targetObject(),
            Set.of(),
            Set.of()
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("subject must not be null");
    }

    @Test
    void rejectsAction_whenTargetObjectIsNull() {
        assertThatThrownBy(() -> new Action(
            ElementId.create(),
            "shall export",
            subject(),
            null,
            Set.of(),
            Set.of()
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("targetObject must not be null");
    }

    @Test
    void rejectsAction_whenConditionsAreNull() {
        assertThatThrownBy(() -> new Action(
            ElementId.create(),
            "shall export",
            subject(),
            targetObject(),
            null,
            Set.of()
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("conditions must not be null");
    }

    @Test
    void rejectsAction_whenConstraintsAreNull() {
        assertThatThrownBy(() -> new Action(
            ElementId.create(),
            "shall export",
            subject(),
            targetObject(),
            Set.of(),
            null
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("constraints must not be null");
    }

    private Subject subject() {
        return new Subject(ElementId.create(), "reporting dashboard");
    }

    private TargetObject targetObject() {
        return new TargetObject(ElementId.create(), "monthly usage metrics");
    }
}
