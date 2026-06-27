package io.fekav.req.syntaxextraction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConstraintTest {

    @Test
    void acceptsConstraint_whenTextIsValid() {
        Constraint constraint = new Constraint(" as CSV ");

        assertThat(constraint.text()).isEqualTo("as CSV");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void rejectsConstraint_whenTextIsBlank(String text) {
        assertThatThrownBy(() -> new Constraint(text))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("constraint text must not be blank");
    }

    @Test
    void rejectsConstraint_whenTextIsNull() {
        assertThatThrownBy(() -> new Constraint(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("constraint text must not be blank");
    }
}
