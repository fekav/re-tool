package io.fekav.req.syntaxextraction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.fekav.req.extraction.domain.Subject;
import io.fekav.req.shared.model.ElementId;

class SubjectTest {

    @Test
    void acceptsSubject_whenIdAndTextAreValid() {
        ElementId id = ElementId.create();

        Subject subject = new Subject(id, " reporting dashboard ");

        assertThat(subject.id()).isEqualTo(id);
        assertThat(subject.text()).isEqualTo("reporting dashboard");
    }

    @Test
    void rejectsSubject_whenIdIsNull() {
        assertThatThrownBy(() -> new Subject(null, "reporting dashboard"))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("id must not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void rejectsSubject_whenTextIsBlank(String text) {
        assertThatThrownBy(() -> new Subject(ElementId.create(), text))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("subject text must not be blank");
    }

    @Test
    void rejectsSubject_whenTextIsNull() {
        assertThatThrownBy(() -> new Subject(ElementId.create(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("subject text must not be blank");
    }
}
