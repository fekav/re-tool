package io.fekav.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CorrelationIdTest {

    @Test
    void createsCorrelationIdWithUuidValue() {
        // Act
        CorrelationId correlationId = CorrelationId.create();

        // Assert
        assertThat(correlationId.value()).isNotNull();
    }

    @Test
    void rejectsCorrelationId_whenValueIsNull() {
        // Act / Assert
        assertThatThrownBy(() -> new CorrelationId(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("correlation id value must not be null");
    }
}
