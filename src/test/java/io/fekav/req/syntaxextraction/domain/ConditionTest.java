package io.fekav.req.syntaxextraction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConditionTest {

    @Test
    void acceptsCondition_whenTextIsValid() {
        Condition condition = new Condition(" customer cancels before shipment ");

        assertThat(condition.text()).isEqualTo("customer cancels before shipment");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void rejectsCondition_whenTextIsBlank(String text) {
        assertThatThrownBy(() -> new Condition(text))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("condition text must not be blank");
    }

    @Test
    void rejectsCondition_whenTextIsNull() {
        assertThatThrownBy(() -> new Condition(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("condition text must not be blank");
    }
}
