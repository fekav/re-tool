package io.fekav.req.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RawRequirementTextTest {

    @Test
    void storesTrimmedText_whenTextHasSurroundingWhitespace() {
        RawRequirementText rawRequirementText = new RawRequirementText("  The system shall export reports.  ");

        assertThat(rawRequirementText.text()).isEqualTo("The system shall export reports.");
    }

    @Test
    void throwsInvalidRawRequirementText_whenTextIsBlank() {
        assertThatThrownBy(() -> new RawRequirementText("   "))
            .isInstanceOf(InvalidRawRequirementTextException.class)
            .hasMessage("raw requirement text must not be blank");
    }
}
