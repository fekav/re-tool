package io.fekav.req.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OriginalTextTest {

    @Test
    void preservesSubmittedTextExactly() {
        // Arrange
        String submittedText = "  The system shall export reports.  ";

        // Act
        OriginalText originalText = new OriginalText(submittedText);

        // Assert
        assertThat(originalText.text()).isEqualTo(submittedText);
    }

    @Test
    void throwsInvalidOriginalText_whenTextIsBlank() {
        // Act / Assert
        assertThatThrownBy(() -> new OriginalText("   "))
            .isInstanceOf(InvalidOriginalTextException.class)
            .hasMessage("original text must not be blank");
    }
}
