package io.fekav.req.syntaxextraction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.fekav.req.shared.model.ElementId;

class TargetObjectTest {

    @Test
    void acceptsTargetObject_whenIdAndTextAreValid() {
        ElementId id = ElementId.create();

        TargetObject targetObject = new TargetObject(id, " monthly usage metrics ");

        assertThat(targetObject.id()).isEqualTo(id);
        assertThat(targetObject.text()).isEqualTo("monthly usage metrics");
    }

    @Test
    void rejectsTargetObject_whenIdIsNull() {
        assertThatThrownBy(() -> new TargetObject(null, "monthly usage metrics"))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("id must not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void rejectsTargetObject_whenTextIsBlank(String text) {
        assertThatThrownBy(() -> new TargetObject(ElementId.create(), text))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("target object text must not be blank");
    }

    @Test
    void rejectsTargetObject_whenTextIsNull() {
        assertThatThrownBy(() -> new TargetObject(ElementId.create(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("target object text must not be blank");
    }
}
