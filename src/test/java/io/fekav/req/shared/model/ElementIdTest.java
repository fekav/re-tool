package io.fekav.req.shared.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ElementIdTest {

    @Test
    void createsElementIdWithUuidValue() {
        ElementId elementId = ElementId.create();

        assertThat(elementId.value()).isNotNull();
    }
}
